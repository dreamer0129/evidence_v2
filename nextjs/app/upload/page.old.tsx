"use client";

import { useState } from "react";
import type { NextPage } from "next";
import { DocumentArrowUpIcon, HashtagIcon } from "@heroicons/react/24/outline";
import { FileUpload } from "~~/components/ui/file-upload";
import { useScaffoldWriteContract } from "~~/hooks/scaffold-eth";
import { calculateSHA256, copyToClipboard, formatFileSize } from "~~/lib/utils";
import { notification } from "~~/utils/scaffold-eth";

interface FileInfo {
  file: File;
  hash: string;
  isLoading: boolean;
  error?: string;
}

interface SubmissionResult {
  evidenceId: string;
  transactionHash: string;
}

const Upload: NextPage = () => {
  const [activeTab, setActiveTab] = useState("file");
  const [fileInfo, setFileInfo] = useState<FileInfo | null>(null);
  const [copied, setCopied] = useState(false);
  const [fileDescription, setFileDescription] = useState("");
  const [hashInput, setHashInput] = useState("");
  const [hashDescription, setHashDescription] = useState("");
  const [submissionResult, setSubmissionResult] = useState<SubmissionResult | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { writeContractAsync: submitEvidence, isMining: isSubmittingEvidence } = useScaffoldWriteContract({
    contractName: "EvidenceStorageContract",
  });

  const { writeContractAsync: submitHashEvidence, isMining: isSubmittingHashEvidence } = useScaffoldWriteContract({
    contractName: "EvidenceStorageContract",
  });

  const handleFileUpload = async (files: File[]) => {
    if (files.length === 0) return;

    const file = files[0]; // 只处理第一个文件

    setFileInfo({
      file,
      hash: "",
      isLoading: true,
    });

    try {
      const hash = await calculateSHA256(file);
      setFileInfo({
        file,
        hash,
        isLoading: false,
      });
    } catch {
      setFileInfo({
        file,
        hash: "",
        isLoading: false,
        error: "哈希计算失败",
      });
    }
  };

  const handleCopyHash = async () => {
    if (!fileInfo?.hash) return;

    const success = await copyToClipboard(fileInfo.hash);
    if (success) {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleSubmitFileEvidence = async () => {
    if (!fileInfo?.hash || fileInfo.isLoading || fileInfo.error) {
      notification.error("请先上传文件并等待哈希计算完成");
      return;
    }

    try {
      setIsSubmitting(true);
      console.log("🚀 开始提交存证流程...");

      // Convert hex hash to bytes32 format
      const hashBytes32 = `0x${fileInfo.hash}` as `0x${string}`;
      console.log("📋 哈希值转换完成:", hashBytes32);

      // Prepare file metadata struct
      const metadata = {
        fileName: fileInfo.file.name,
        mimeType: fileInfo.file.type || "",
        size: BigInt(fileInfo.file.size),
        creationTime: BigInt(fileInfo.file.lastModified),
      };
      console.log("📄 文件元数据准备完成:", metadata);

      // Prepare hash info struct
      const hashInfo = {
        algorithm: "SHA256",
        value: hashBytes32,
      };
      console.log("🔐 哈希信息准备完成:", hashInfo);

      console.log("⏳ 准备调用 submitEvidence 合约函数...");
      const startTime = Date.now();
      console.log("📝 调用参数:", {
        functionName: "submitEvidence",
        args: [metadata, hashInfo, fileDescription],
      });

      console.log("💰 等待钱包确认... (这里应该弹出 MetaMask)");
      const walletStartTime = Date.now();

      const result = await submitEvidence({
        functionName: "submitEvidence",
        args: [metadata, hashInfo, fileDescription],
      });

      const walletEndTime = Date.now();
      console.log(`✅ 钱包确认完成, 耗时: ${walletEndTime - walletStartTime}ms`);
      console.log(`📊 总调用耗时: ${walletEndTime - startTime}ms`);

      if (result) {
        console.log("🎉 交易提交成功, 交易哈希:", result);
        setSubmissionResult({
          evidenceId: "", // Will be retrieved from transaction receipt
          transactionHash: result,
        });
        notification.success("存证提交成功!");
        // Reset form
        setFileInfo(null);
        setFileDescription("");
      }
    } catch (error: any) {
      console.error("❌ Submit evidence error:", error);
      console.error("📊 Error details:", {
        name: error.name,
        message: error.message,
        code: error.code,
        stack: error.stack,
      });
      notification.error(`存证提交失败: ${error.message || "未知错误"}`);
    } finally {
      setIsSubmitting(false);
      console.log("🏁 提交流程结束");
    }
  };

  const handleSubmitHashOnly = async () => {
    if (!hashInput.trim()) {
      notification.error("请输入哈希值");
      return;
    }

    // Validate hash format (64 character hex string)
    const hashRegex = /^[a-fA-F0-9]{64}$/;
    if (!hashRegex.test(hashInput.trim())) {
      notification.error("请输入有效的64位SHA256哈希值");
      return;
    }

    try {
      setIsSubmitting(true);

      // Convert hex hash to bytes32 format
      const hashBytes32 = `0x${hashInput.trim()}` as `0x${string}`;

      // Prepare hash info struct
      const hashInfo = {
        algorithm: "SHA256",
        value: hashBytes32,
      };

      const result = await submitHashEvidence({
        functionName: "submitHashEvidence",
        args: [hashDescription || "用户提交的哈希存证", hashInfo, hashDescription],
      });

      if (result) {
        setSubmissionResult({
          evidenceId: "", // Will be retrieved from transaction receipt
          transactionHash: result,
        });
        notification.success("哈希存证提交成功!");
        // Reset form
        setHashInput("");
        setHashDescription("");
      }
    } catch (error: any) {
      console.error("Submit hash evidence error:", error);
      notification.error(`哈希存证提交失败: ${error.message || "未知错误"}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6 lg:p-8">
      <div className="text-center">
        <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl">创建您的数字存证</h1>
        <p className="mt-3 text-lg text-gray-600">安全、快速地将您的重要文件或数据哈希上链</p>
      </div>

      {submissionResult && (
        <div className="mt-6 bg-green-50 border border-green-200 rounded-lg p-4">
          <h3 className="font-semibold text-green-800 mb-3">存证提交成功!</h3>
          <div className="space-y-2 text-sm">
            <div>
              <span className="text-green-600">交易哈希:</span>
              <p className="font-mono text-xs break-all">{submissionResult.transactionHash}</p>
            </div>
            {submissionResult.evidenceId && (
              <div>
                <span className="text-green-600">存证ID:</span>
                <p className="font-medium">{submissionResult.evidenceId}</p>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="mt-12">
        <div className="tabs tabs-boxed bg-base-200 p-2 rounded-lg justify-center">
          <a className={`tab tab-lg ${activeTab === "file" ? "tab-active" : ""}`} onClick={() => setActiveTab("file")}>
            <DocumentArrowUpIcon className="w-6 h-6 mr-2" />
            文件上传
          </a>
          <a className={`tab tab-lg ${activeTab === "hash" ? "tab-active" : ""}`} onClick={() => setActiveTab("hash")}>
            <HashtagIcon className="w-6 h-6 mr-2" />
            哈希上传
          </a>
        </div>
      </div>

      <div className="mt-6">
        <div className="card bg-base-100 shadow-xl w-full">
          <div className="card-body">
            {activeTab === "file" && (
              <div className="space-y-6">
                <div>
                  <h2 className="text-xl font-semibold">文件上传存证</h2>
                  <p className="text-gray-500 mt-1">选择您要存证的文件，系统将自动计算哈希值并上链。</p>
                </div>
                <div className="form-control">
                  <label className="block text-sm font-medium text-gray-700 mb-2">选择文件</label>
                  <FileUpload onChange={handleFileUpload} />
                  <p className="text-xs text-gray-500 mt-2 text-center">支持 PDF, DOC, JPG, PNG 等格式, 最大 100MB</p>
                </div>

                {fileInfo && (
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mt-4">
                    <h3 className="font-semibold text-blue-800 mb-3">文件信息</h3>

                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-gray-600">文件名:</span>
                        <p className="font-medium truncate">{fileInfo.file.name}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">文件大小:</span>
                        <p className="font-medium">{formatFileSize(fileInfo.file.size)}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">文件类型:</span>
                        <p className="font-medium">{fileInfo.file.type || "未知类型"}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">修改时间:</span>
                        <p className="font-medium">{new Date(fileInfo.file.lastModified).toLocaleString("zh-CN")}</p>
                      </div>
                    </div>

                    <div className="mt-4 pt-4 border-t border-blue-100">
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-gray-600">SHA256 哈希值:</span>
                        {fileInfo.isLoading ? (
                          <div className="flex items-center">
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600 mr-2"></div>
                            <span className="text-blue-600 text-sm">计算中...</span>
                          </div>
                        ) : fileInfo.error ? (
                          <span className="text-red-600 text-sm">{fileInfo.error}</span>
                        ) : (
                          <button
                            onClick={handleCopyHash}
                            className="flex items-center text-sm text-blue-600 hover:text-blue-800 transition-colors"
                            title="复制哈希值"
                          >
                            {copied ? (
                              <>
                                <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                                  <path
                                    fillRule="evenodd"
                                    d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                                    clipRule="evenodd"
                                  />
                                </svg>
                                已复制
                              </>
                            ) : (
                              <>
                                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
                                  />
                                </svg>
                                复制
                              </>
                            )}
                          </button>
                        )}
                      </div>

                      {!fileInfo.isLoading && !fileInfo.error && fileInfo.hash && (
                        <div className="bg-blue-100 p-3 rounded">
                          <code className="text-blue-800 text-xs font-mono break-all">
                            {fileInfo.hash.slice(0, 32)}...{fileInfo.hash.slice(-16)}
                          </code>
                          <div className="flex justify-between items-center mt-2 text-xs text-gray-600">
                            <span>共 {fileInfo.hash.length} 位</span>
                            <span>前32位...后16位</span>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                )}
                <div className="form-control">
                  <label htmlFor="file-description" className="block text-sm font-medium text-gray-700">
                    文件描述 (可选)
                  </label>
                  <textarea
                    id="file-description"
                    value={fileDescription}
                    onChange={e => setFileDescription(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 mt-1"
                    placeholder="例如: 2023年第四季度财务报表"
                    rows={3}
                  ></textarea>
                </div>
                <div className="card-actions justify-end">
                  <button
                    className="btn btn-primary btn-lg"
                    onClick={handleSubmitFileEvidence}
                    disabled={
                      isSubmitting || isSubmittingEvidence || !fileInfo?.hash || fileInfo.isLoading || !!fileInfo.error
                    }
                  >
                    {isSubmitting || isSubmittingEvidence ? (
                      <>
                        <span className="loading loading-spinner loading-sm"></span>
                        提交中...
                      </>
                    ) : (
                      "提交存证"
                    )}
                  </button>
                </div>
              </div>
            )}

            {activeTab === "hash" && (
              <div className="space-y-6">
                <div>
                  <h2 className="text-xl font-semibold">哈希上传存证</h2>
                  <p className="text-gray-500 mt-1">如果您已在本地计算好文件哈希，可直接提交哈希值进行存证。</p>
                </div>
                <div className="form-control">
                  <label htmlFor="hash-input" className="block text-sm font-medium text-gray-700">
                    文件哈希 (SHA256)
                  </label>
                  <input
                    id="hash-input"
                    type="text"
                    value={hashInput}
                    onChange={e => setHashInput(e.target.value)}
                    placeholder="请输入 64 位的 SHA256 哈希值"
                    className="input input-bordered w-full mt-1 font-mono"
                    maxLength={64}
                  />
                </div>
                <div className="form-control">
                  <label htmlFor="hash-description" className="block text-sm font-medium text-gray-700">
                    文件描述 (可选)
                  </label>
                  <textarea
                    id="hash-description"
                    value={hashDescription}
                    onChange={e => setHashDescription(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 mt-1"
                    placeholder="例如: 原始文件名或相关业务编号"
                    rows={3}
                  ></textarea>
                </div>
                <div className="card-actions justify-end">
                  <button
                    className="btn btn-primary btn-lg"
                    onClick={handleSubmitHashOnly}
                    disabled={isSubmitting || isSubmittingHashEvidence || !hashInput.trim()}
                  >
                    {isSubmitting || isSubmittingHashEvidence ? (
                      <>
                        <span className="loading loading-spinner loading-sm"></span>
                        提交中...
                      </>
                    ) : (
                      "提交存证"
                    )}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Upload;
