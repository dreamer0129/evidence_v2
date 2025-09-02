"use client";

import { useState } from "react";
import { CheckCircle, FileText, Hash as HashIcon, Loader2, Upload as UploadIcon } from "lucide-react";
import type { NextPage } from "next";
import { FileUpload } from "~~/components/evidence/FileUpload";
import { GlassButton, GlassCard } from "~~/components/evidence/GlassContainer";
import { PageBackgroundWrapper } from "~~/components/evidence/PageBackgroundWrapper";
import { useScaffoldWriteContract } from "~~/hooks/scaffold-eth";
import { calculateSHA256, copyToClipboard } from "~~/lib/utils";
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
    contractName: "EvidenceStorage",
  });

  const { writeContractAsync: submitHashEvidence, isMining: isSubmittingHashEvidence } = useScaffoldWriteContract({
    contractName: "EvidenceStorage",
  });

  const handleFileUpload = async (files: File[]) => {
    if (files.length === 0) {
      // Clear file info when no files are selected
      setFileInfo(null);
      return;
    }

    const file = files[0];

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
        notification.success("存证提交成功！");
        // Reset form
        setFileInfo(null);
        setFileDescription("");
      }
    } catch (error: any) {
      console.error("❌ 存证提交失败:", error);
      notification.error(`存证提交失败: ${error.message || "未知错误"}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitHashOnly = async () => {
    if (!hashInput.trim()) {
      notification.error("请输入哈希值");
      return;
    }

    // Validate hash format
    const hashRegex = /^[a-fA-F0-9]{64}$/;
    if (!hashRegex.test(hashInput)) {
      notification.error("请输入有效的64位SHA256哈希值");
      return;
    }

    try {
      setIsSubmitting(true);
      console.log("🚀 开始提交哈希存证...");

      // Convert hex hash to bytes32 format
      const hashBytes32 = `0x${hashInput}` as `0x${string}`;
      console.log("📋 哈希值转换完成:", hashBytes32);

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
        notification.success("哈希存证提交成功！");
        // Reset form
        setHashInput("");
        setHashDescription("");
      }
    } catch (error: any) {
      console.error("❌ 哈希存证提交失败:", error);
      notification.error(`哈希存证提交失败: ${error.message || "未知错误"}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <PageBackgroundWrapper>
      <div className="min-h-screen pt-24 pb-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-black dark:text-white mb-2">创建您的数字存证</h1>
            <p className="text-base text-gray-800 dark:text-gray-300">安全、快速地将您的重要文件或数据哈希上链</p>
          </div>

          {/* Success Notification */}
          {submissionResult && (
            <div className="mb-6">
              <GlassCard intensity="high" className="border-green-200/50">
                <div className="flex items-start space-x-3">
                  <div className="flex-shrink-0">
                    <CheckCircle className="w-6 h-6 text-green-600 dark:text-green-400" />
                  </div>
                  <div className="flex-1">
                    <h3 className="font-semibold text-green-800 dark:text-green-200 mb-2">存证提交成功!</h3>
                    <div className="space-y-2 text-sm">
                      <div>
                        <span className="text-green-600 dark:text-green-400">交易哈希:</span>
                        <p className="font-mono text-xs break-all text-gray-700 dark:text-gray-300 mt-1">
                          {submissionResult.transactionHash}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </GlassCard>
            </div>
          )}

          {/* Tab Navigation */}
          <div className="mb-6">
            <GlassCard intensity="low" className="p-2">
              <div className="flex space-x-1">
                <button
                  className={`flex-1 flex items-center justify-center space-x-2 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-300 ${
                    activeTab === "file"
                      ? "bg-white/50 dark:bg-black/50 text-blue-600 dark:text-blue-400 shadow-sm"
                      : "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                  }`}
                  onClick={() => setActiveTab("file")}
                >
                  <UploadIcon width={20} height={20} />
                  <span>文件上传</span>
                </button>
                <button
                  className={`flex-1 flex items-center justify-center space-x-2 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-300 ${
                    activeTab === "hash"
                      ? "bg-white/50 dark:bg-black/50 text-blue-600 dark:text-blue-400 shadow-sm"
                      : "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                  }`}
                  onClick={() => setActiveTab("hash")}
                >
                  <HashIcon width={20} height={20} />
                  <span>哈希上传</span>
                </button>
              </div>
            </GlassCard>
          </div>

          {/* Content */}
          <div>
            <GlassCard>
              {activeTab === "file" ? (
                <div className="p-6 space-y-6">
                  <div className="text-center mb-6">
                    <h2 className="text-2xl font-semibold text-gray-900 dark:text-white mb-2">文件上传存证</h2>
                    <p className="text-gray-600 dark:text-gray-300">选择您要存证的文件，系统将自动计算哈希值并上链</p>
                  </div>

                  <FileUpload onChange={handleFileUpload} />

                  {fileInfo && (
                    <GlassCard intensity="medium" className="overflow-hidden">
                      {/* File Header */}
                      <div className="bg-gradient-to-r from-blue-500/10 to-purple-500/10 px-4 py-3 border-b border-white/10">
                        <div className="flex items-center space-x-2">
                          <div className="p-1.5 bg-blue-500/20 rounded-lg">
                            <FileText className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                          </div>
                          <h3 className="font-semibold text-gray-900 dark:text-white text-sm">文件信息</h3>
                        </div>
                      </div>

                      {/* File Details */}
                      <div className="p-3 space-y-3">
                        {/* File Name */}
                        <div>
                          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider block">
                            文件名
                          </label>
                          <div className="bg-gray-50 dark:bg-gray-800/50 rounded-lg px-3 py-1.5 border border-gray-200 dark:border-gray-700 mt-1">
                            <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                              {fileInfo.file.name}
                            </p>
                          </div>
                        </div>

                        {/* File Size & Type */}
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <label className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider block">
                              文件大小
                            </label>
                            <div className="bg-gray-50 dark:bg-gray-800/50 rounded-lg px-3 py-1.5 border border-gray-200 dark:border-gray-700 mt-1">
                              <p className="text-sm font-medium text-gray-900 dark:text-white">
                                {(fileInfo.file.size / 1024 / 1024).toFixed(2)} MB
                              </p>
                            </div>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider block">
                              文件类型
                            </label>
                            <div className="bg-gray-50 dark:bg-gray-800/50 rounded-lg px-3 py-1.5 border border-gray-200 dark:border-gray-700 mt-1">
                              <p className="text-sm font-medium text-gray-900 dark:text-white">
                                {fileInfo.file.type || "未知"}
                              </p>
                            </div>
                          </div>
                        </div>

                        {/* Hash Value */}
                        <div>
                          <div className="flex items-center justify-between">
                            <label className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                              SHA256 哈希值
                            </label>
                            <button
                              onClick={handleCopyHash}
                              className="text-xs font-medium text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 transition-colors"
                            >
                              {copied ? "✓ 已复制" : "复制"}
                            </button>
                          </div>
                          <div className="bg-gray-50 dark:bg-gray-800/50 rounded-lg px-3 py-1.5 border border-gray-200 dark:border-gray-700 mt-1">
                            <p className="text-sm font-mono text-gray-900 dark:text-white break-all leading-tight">
                              {fileInfo.hash}
                            </p>
                          </div>
                        </div>

                        {/* Status Indicator */}
                        <div className="flex items-center space-x-2 bg-green-50 dark:bg-green-900/20 rounded-lg px-3 py-1.5 border border-green-200 dark:border-green-800">
                          <CheckCircle className="w-3.5 h-3.5 text-green-600 dark:text-green-400 flex-shrink-0" />
                          <span className="text-xs font-medium text-green-800 dark:text-green-200">
                            文件验证成功，哈希值已计算
                          </span>
                        </div>
                      </div>
                    </GlassCard>
                  )}

                  <GlassCard intensity="medium" className="p-6">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      文件描述 (可选)
                    </label>
                    <textarea
                      value={fileDescription}
                      onChange={e => setFileDescription(e.target.value)}
                      className="w-full px-4 py-3 bg-white/10 dark:bg-black/10 border border-white/20 rounded-xl 
                               text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400
                               focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                               resize-none transition-all duration-300"
                      placeholder="例如: 2023年第四季度财务报表"
                      rows={3}
                    />
                  </GlassCard>

                  <div className="flex justify-center">
                    <GlassButton
                      onClick={handleSubmitFileEvidence}
                      disabled={
                        isSubmitting ||
                        isSubmittingEvidence ||
                        !fileInfo?.hash ||
                        fileInfo.isLoading ||
                        !!fileInfo.error
                      }
                    >
                      {isSubmitting || isSubmittingEvidence ? (
                        <>
                          <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                          提交中...
                        </>
                      ) : (
                        "提交存证"
                      )}
                    </GlassButton>
                  </div>
                </div>
              ) : (
                <div className="p-6 space-y-6">
                  <div className="text-center mb-6">
                    <h2 className="text-2xl font-semibold text-gray-900 dark:text-white mb-2">哈希上传存证</h2>
                    <p className="text-gray-600 dark:text-gray-300">
                      如果您已在本地计算好文件哈希，可直接提交哈希值进行存证
                    </p>
                  </div>

                  <GlassCard intensity="medium" className="p-6">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      文件哈希 (SHA256)
                    </label>
                    <input
                      type="text"
                      value={hashInput}
                      onChange={e => setHashInput(e.target.value)}
                      placeholder="请输入 64 位的 SHA256 哈希值"
                      className="w-full px-4 py-3 bg-white/10 dark:bg-black/10 border border-white/20 rounded-xl 
                               text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400
                               focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                               font-mono transition-all duration-300"
                      maxLength={64}
                    />
                  </GlassCard>

                  <GlassCard intensity="medium" className="p-6">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      文件描述 (可选)
                    </label>
                    <textarea
                      value={hashDescription}
                      onChange={e => setHashDescription(e.target.value)}
                      className="w-full px-4 py-3 bg-white/10 dark:bg-black/10 border border-white/20 rounded-xl 
                               text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400
                               focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                               resize-none transition-all duration-300"
                      placeholder="例如: 原始文件名或相关业务编号"
                      rows={3}
                    />
                  </GlassCard>

                  <div className="flex justify-center">
                    <GlassButton
                      onClick={handleSubmitHashOnly}
                      disabled={isSubmitting || isSubmittingHashEvidence || !hashInput.trim()}
                    >
                      {isSubmitting || isSubmittingHashEvidence ? (
                        <>
                          <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                          提交中...
                        </>
                      ) : (
                        "提交存证"
                      )}
                    </GlassButton>
                  </div>
                </div>
              )}
            </GlassCard>
          </div>
        </div>
      </div>
    </PageBackgroundWrapper>
  );
};

export default Upload;
