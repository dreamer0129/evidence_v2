"use client";

import { useState } from "react";
import type { NextPage } from "next";

interface VerificationResult {
  success: boolean;
  fileName: string;
  timestamp: string;
  fileHash: string;
  txHash: string;
}

const Verify: NextPage = () => {
  const [activeTab, setActiveTab] = useState("file");
  const [verificationResult, setVerificationResult] = useState<VerificationResult | null>(null);

  const handleVerification = () => {
    // Mock verification logic
    setVerificationResult({
      success: true,
      fileName: "合同-2023-A.pdf",
      timestamp: "2023-10-26 10:30:00",
      fileHash: "a1b2c3d4...e5f6",
      txHash: "0x1234...5678",
    });
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
              <input type="file" className="file-input file-input-bordered w-full" />
            </div>
            <button className="btn btn-primary mt-6" onClick={handleVerification}>
              验证
            </button>
          </div>
        )}

        {activeTab === "hash" && (
          <div>
            <div className="form-control w-full">
              <label className="label">
                <span className="label-text">输入文件哈希 (SHA256)</span>
              </label>
              <input type="text" placeholder="请输入要验证的文件哈希值" className="input input-bordered w-full" />
            </div>
            <button className="btn btn-primary mt-6" onClick={handleVerification}>
              验证
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
                <p className="text-success">验证成功！文件哈希值匹配，存证有效。</p>
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
                        <td className="font-mono">{verificationResult.fileHash}</td>
                      </tr>
                      <tr>
                        <th>交易哈希</th>
                        <td className="font-mono">{verificationResult.txHash}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <p className="text-error">验证失败！未找到匹配的存证记录。</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default Verify;
