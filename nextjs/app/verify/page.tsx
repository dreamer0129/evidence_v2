"use client";

import { useEffect, useState } from "react";
import { Hash, Upload } from "lucide-react";
import type { NextPage } from "next";
import { FileUpload } from "~~/components/evidence/FileUpload";
import { GlassContainer } from "~~/components/evidence/GlassContainer";
import { GlassCard } from "~~/components/evidence/GlassContainer";
import { PageBackgroundWrapper } from "~~/components/evidence/PageBackgroundWrapper";
import { VerificationResultCard } from "~~/components/evidence/VerificationResultCard";
import { useScaffoldReadContract } from "~~/hooks/scaffold-eth";
import { calculateSHA256 } from "~~/lib/utils";

interface VerificationResult {
  success: boolean;
  fileName: string;
  timestamp: string;
  fileHash: string;
  evidenceId: string;
  blockHeight: number;
  status: string;
  error?: string;
}

const Verify: NextPage = () => {
  const [activeTab, setActiveTab] = useState("file");
  const [file, setFile] = useState<File | null>(null);
  const [hashInput, setHashInput] = useState("");
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationResult, setVerificationResult] = useState<VerificationResult | null>(null);

  const [targetHash, setTargetHash] = useState<`0x${string}` | null>(null);
  const [hashValue, setHashValue] = useState<string>("");

  const { data: isValid } = useScaffoldReadContract({
    contractName: "EvidenceStorage",
    functionName: "verifyEvidenceByHash",
    args: targetHash ? [targetHash] : ([] as any),
  });

  const { data: evidence } = useScaffoldReadContract({
    contractName: "EvidenceStorage",
    functionName: "getEvidenceByHash",
    args: targetHash ? [targetHash] : ([] as any),
  });

  useEffect(() => {
    if (targetHash && isValid !== undefined) {
      if (isValid === false) {
        setVerificationResult({
          success: false,
          fileName: "",
          timestamp: "",
          fileHash: hashValue,
          evidenceId: "",
          blockHeight: 0,
          status: "",
          error: "未找到匹配的存证记录或存证已失效",
        });
        setIsVerifying(false);
        setTargetHash(null);
      } else if (isValid === true && evidence) {
        setVerificationResult({
          success: true,
          fileName: evidence.metadata.fileName,
          timestamp: new Date(Number(evidence.timestamp) * 1000).toLocaleString("zh-CN"),
          fileHash: hashValue,
          evidenceId: evidence.evidenceId,
          blockHeight: Number(evidence.blockHeight),
          status: evidence.status,
        });
        setIsVerifying(false);
        setTargetHash(null);
      }
    }
  }, [targetHash, isValid, evidence, hashValue]);

  const handleFileUpload = (files: File[]) => {
    if (files.length > 0) {
      setFile(files[0]);
    }
  };

  const handleVerification = async () => {
    setIsVerifying(true);
    setVerificationResult(null);

    try {
      let hashValue: string;

      if (activeTab === "file" && file) {
        // Calculate hash from uploaded file
        hashValue = await calculateSHA256(file);
      } else if (activeTab === "hash" && hashInput.trim()) {
        // Use provided hash
        hashValue = hashInput.trim();
        // Validate hash format
        const hashRegex = /^[a-fA-F0-9]{64}$/;
        if (!hashRegex.test(hashValue)) {
          throw new Error("请输入有效的64位SHA256哈希值");
        }
      } else {
        throw new Error(activeTab === "file" ? "请选择要验证的文件" : "请输入要验证的哈希值");
      }

      // Store the hash value and set target hash to trigger queries
      setHashValue(hashValue);
      const hashBytes32 = `0x${hashValue}` as `0x${string}`;
      setTargetHash(hashBytes32);
    } catch (error: any) {
      console.error("Verification error:", error);
      setVerificationResult({
        success: false,
        fileName: "",
        timestamp: "",
        fileHash: "",
        evidenceId: "",
        blockHeight: 0,
        status: "",
        error: error.message || "验证过程中发生错误",
      });
      setIsVerifying(false);
    }
  };

  return (
    <PageBackgroundWrapper>
      <div className="min-h-screen pt-24 pb-8 px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-black dark:text-white mb-2">存证验证</h1>
          <p className="text-base text-gray-800 dark:text-gray-300">验证文件完整性和区块链存证记录</p>
        </div>

        {/* Main Content */}
        <div className="max-w-4xl mx-auto space-y-8">
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
                  onClick={() => {
                    setActiveTab("file");
                    setVerificationResult(null);
                    setFile(null);
                    setHashInput("");
                  }}
                >
                  <Upload width={20} height={20} />
                  <span>文件验证</span>
                </button>
                <button
                  className={`flex-1 flex items-center justify-center space-x-2 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-300 ${
                    activeTab === "hash"
                      ? "bg-white/50 dark:bg-black/50 text-blue-600 dark:text-blue-400 shadow-sm"
                      : "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200"
                  }`}
                  onClick={() => {
                    setActiveTab("hash");
                    setVerificationResult(null);
                    setFile(null);
                    setHashInput("");
                  }}
                >
                  <Hash width={20} height={20} />
                  <span>哈希验证</span>
                </button>
              </div>
            </GlassCard>
          </div>

          {/* Verification Content */}
          <GlassCard>
            {activeTab === "file" && (
              <div className="space-y-6">
                <div className="text-center mb-6">
                  <h3 className="text-2xl font-semibold text-black dark:text-white mb-2">上传文件进行验证</h3>
                  <p className="text-gray-800 dark:text-gray-300">系统将计算文件哈希值并与区块链上的存证记录进行比对</p>
                </div>

                <FileUpload onChange={handleFileUpload} maxSize={100} acceptedTypes={["*"]} />

                {file && (
                  <div className="flex justify-center">
                    <button
                      onClick={handleVerification}
                      disabled={isVerifying}
                      className="px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-xl font-medium hover:from-blue-600 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300 shadow-lg hover:shadow-xl hover:scale-105"
                    >
                      {isVerifying ? (
                        <div className="flex items-center space-x-2">
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                          <span>验证中...</span>
                        </div>
                      ) : (
                        "开始验证"
                      )}
                    </button>
                  </div>
                )}
              </div>
            )}

            {activeTab === "hash" && (
              <div className="space-y-6">
                <div className="text-center mb-6">
                  <h3 className="text-2xl font-semibold text-black dark:text-white mb-2">输入哈希值进行验证</h3>
                  <p className="text-gray-800 dark:text-gray-300">直接输入文件的SHA256哈希值进行验证</p>
                </div>

                <GlassContainer intensity="low" className="p-6">
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">文件哈希 (SHA256)</label>
                      <textarea
                        value={hashInput}
                        onChange={e => setHashInput(e.target.value)}
                        placeholder="请输入64位十六进制哈希值"
                        className="w-full px-4 py-3 bg-white/10 dark:bg-black/10 border border-white/20 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:border-blue-400 focus:outline-none transition-colors font-mono text-sm"
                        rows={3}
                        maxLength={64}
                      />
                      <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">64位十六进制字符 (0-9, a-f)</p>
                    </div>

                    <div className="flex justify-center">
                      <button
                        onClick={handleVerification}
                        disabled={isVerifying || !hashInput.trim()}
                        className="px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-xl font-medium hover:from-blue-600 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300 shadow-lg hover:shadow-xl hover:scale-105"
                      >
                        {isVerifying ? (
                          <div className="flex items-center space-x-2">
                            <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                            <span>验证中...</span>
                          </div>
                        ) : (
                          "开始验证"
                        )}
                      </button>
                    </div>
                  </div>
                </GlassContainer>
              </div>
            )}
          </GlassCard>

          {/* Verification Result */}
          {(isVerifying || verificationResult) && (
            <VerificationResultCard result={verificationResult} isVerifying={isVerifying} />
          )}

          {/* Info Section */}
          <GlassCard intensity="low">
            <div className="text-center space-y-4">
              <h3 className="text-lg font-semibold text-black dark:text-white">如何验证？</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-left">
                <div className="space-y-2">
                  <div className="w-10 h-10 bg-blue-500/20 rounded-lg flex items-center justify-center mb-2">
                    <span className="text-blue-400 font-bold">1</span>
                  </div>
                  <h4 className="font-medium text-black dark:text-white">选择验证方式</h4>
                  <p className="text-sm text-gray-600 dark:text-gray-400">上传文件或直接输入哈希值</p>
                </div>
                <div className="space-y-2">
                  <div className="w-10 h-10 bg-purple-500/20 rounded-lg flex items-center justify-center mb-2">
                    <span className="text-purple-400 font-bold">2</span>
                  </div>
                  <h4 className="font-medium text-black dark:text-white">系统计算哈希</h4>
                  <p className="text-sm text-gray-600 dark:text-gray-400">自动计算SHA256哈希值</p>
                </div>
                <div className="space-y-2">
                  <div className="w-10 h-10 bg-green-500/20 rounded-lg flex items-center justify-center mb-2">
                    <span className="text-green-400 font-bold">3</span>
                  </div>
                  <h4 className="font-medium text-black dark:text-white">区块链验证</h4>
                  <p className="text-sm text-gray-600 dark:text-gray-400">查询区块链存证记录</p>
                </div>
              </div>
            </div>
          </GlassCard>
        </div>
      </div>
    </PageBackgroundWrapper>
  );
};

export default Verify;
