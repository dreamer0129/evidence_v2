"use client";

import { useEffect, useState } from "react";
import type { NextPage } from "next";
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
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-center mb-8">存证验证</h1>

      <div role="tablist" className="tabs tabs-lifted">
        <a
          role="tab"
          className={`tab ${activeTab === "file" ? "tab-active" : ""}`}
          onClick={() => {
            setActiveTab("file");
            setVerificationResult(null);
          }}
        >
          文件验证
        </a>
        <a
          role="tab"
          className={`tab ${activeTab === "hash" ? "tab-active" : ""}`}
          onClick={() => {
            setActiveTab("hash");
            setVerificationResult(null);
          }}
        >
          哈希验证
        </a>
      </div>

      <div className="bg-base-100 p-6 rounded-box shadow-md">
        {activeTab === "file" && (
          <div>
            <div className="form-control w-full">
              <label className="label">
                <span className="label-text">选择要验证的文件</span>
              </label>
              <input
                type="file"
                className="file-input file-input-bordered w-full"
                onChange={e => handleFileUpload(e.target.files ? Array.from(e.target.files) : [])}
              />
              {file && (
                <div className="mt-2 text-sm text-gray-500">
                  已选择: {file.name} ({file.size} bytes)
                </div>
              )}
            </div>
            <button className="btn btn-primary mt-6" onClick={handleVerification} disabled={isVerifying || !file}>
              {isVerifying ? (
                <>
                  <span className="loading loading-spinner loading-sm"></span>
                  验证中...
                </>
              ) : (
                "验证"
              )}
            </button>
          </div>
        )}

        {activeTab === "hash" && (
          <div>
            <div className="form-control w-full">
              <label className="label">
                <span className="label-text">输入文件哈希 (SHA256)</span>
              </label>
              <input
                type="text"
                placeholder="请输入要验证的文件哈希值"
                className="input input-bordered w-full font-mono"
                value={hashInput}
                onChange={e => setHashInput(e.target.value)}
                maxLength={64}
              />
              <label className="label">
                <span className="label-text-alt text-gray-500">64位十六进制字符 (0-9, a-f)</span>
              </label>
            </div>
            <button
              className="btn btn-primary mt-6"
              onClick={handleVerification}
              disabled={isVerifying || !hashInput.trim()}
            >
              {isVerifying ? (
                <>
                  <span className="loading loading-spinner loading-sm"></span>
                  验证中...
                </>
              ) : (
                "验证"
              )}
            </button>
          </div>
        )}
      </div>

      {verificationResult && (
        <div className="card bg-base-100 shadow-xl mt-8">
          <div className="card-body">
            <h2 className="card-title">验证结果</h2>
            {verificationResult.success ? (
              <div>
                <p className="text-success">✅ 验证成功！文件哈希值匹配，存证有效。</p>
                <div className="overflow-x-auto mt-4">
                  <table className="table w-full">
                    <tbody>
                      <tr>
                        <th>文件名称</th>
                        <td>{verificationResult.fileName}</td>
                      </tr>
                      <tr>
                        <th>存证时间</th>
                        <td>{verificationResult.timestamp}</td>
                      </tr>
                      <tr>
                        <th>文件哈希</th>
                        <td className="font-mono text-xs">{verificationResult.fileHash}</td>
                      </tr>
                      <tr>
                        <th>存证ID</th>
                        <td className="font-mono text-xs">{verificationResult.evidenceId}</td>
                      </tr>
                      <tr>
                        <th>区块高度</th>
                        <td>{verificationResult.blockHeight}</td>
                      </tr>
                      <tr>
                        <th>存证状态</th>
                        <td className="text-success">{verificationResult.status}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div>
                <p className="text-error">❌ 验证失败！</p>
                <p className="text-gray-600 mt-2">{verificationResult.error}</p>
                {verificationResult.fileHash && (
                  <div className="mt-4 p-3 bg-gray-100 rounded">
                    <p className="text-sm text-gray-700">查询的哈希值:</p>
                    <code className="text-xs font-mono break-all">{verificationResult.fileHash}</code>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default Verify;
