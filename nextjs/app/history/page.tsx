"use client";

import { useState } from "react";
import { EvidenceTableRow } from "./EvidenceTableRow";
import type { NextPage } from "next";
import { useAccount } from "wagmi";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "~~/components/ui/table";
import { useScaffoldReadContract } from "~~/hooks/scaffold-eth";
import { type EvidenceDetails } from "~~/hooks/scaffold-eth/useEvidenceDetails";

const History: NextPage = () => {
  const { address } = useAccount();
  const [selectedEvidence, setSelectedEvidence] = useState<EvidenceDetails | null>(null);

  // Fetch user's evidence IDs
  const { data: evidenceIds, isLoading: loadingIds } = useScaffoldReadContract({
    contractName: "EvidenceStorage",
    functionName: "getUserEvidences",
    args: [address],
  });

  if (!address) {
    return (
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-center mb-8">我的存证记录</h1>
        <div className="text-center text-gray-500">请连接钱包查看存证记录</div>
      </div>
    );
  }

  if (loadingIds) {
    return (
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-center mb-8">我的存证记录</h1>
        <div className="text-center">
          <div className="loading loading-spinner loading-lg"></div>
          <p className="mt-4 text-gray-500">加载存证记录中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-center mb-8">我的存证记录</h1>

      {!evidenceIds || evidenceIds.length === 0 ? (
        <div className="text-center text-gray-500">暂无存证记录</div>
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>序号</TableHead>
                <TableHead>证据ID</TableHead>
                <TableHead>文件名</TableHead>
                <TableHead>文件大小</TableHead>
                <TableHead>存证时间</TableHead>
                <TableHead>文件类型</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {evidenceIds?.map((evidenceId, index) => (
                <EvidenceTableRow
                  key={evidenceId}
                  evidenceId={evidenceId}
                  index={index}
                  onViewDetails={setSelectedEvidence}
                />
              ))}
            </TableBody>
          </Table>
          <div className="mt-4 text-sm text-gray-500 text-center">共 {evidenceIds.length} 条存证记录</div>
        </div>
      )}

      {/* Evidence Detail Modal */}
      {selectedEvidence && (
        <div className="modal modal-open">
          <div className="modal-box max-w-4xl">
            <h3 className="font-bold text-lg mb-4">证据详情</h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <div>
                  <strong>证据ID:</strong> <span className="font-mono text-sm">{selectedEvidence.evidenceId}</span>
                </div>
                <div>
                  <strong>文件名:</strong> {selectedEvidence.metadata.fileName || "-"}
                </div>
                <div>
                  <strong>文件大小:</strong>{" "}
                  {selectedEvidence.metadata.size > 0
                    ? `${(Number(selectedEvidence.metadata.size) / 1024).toFixed(1)} KB`
                    : "-"}
                </div>
                <div>
                  <strong>文件类型:</strong> {selectedEvidence.metadata.mimeType || "-"}
                </div>
                <div>
                  <strong>创建时间:</strong>{" "}
                  {new Date(Number(selectedEvidence.metadata.creationTime) * 1000).toLocaleString("zh-CN")}
                </div>
              </div>

              <div className="space-y-2">
                <div>
                  <strong>存证时间:</strong>{" "}
                  {new Date(Number(selectedEvidence.timestamp) * 1000).toLocaleString("zh-CN")}
                </div>
                <div>
                  <strong>区块高度:</strong> {selectedEvidence.blockHeight.toString()}
                </div>
                <div>
                  <strong>状态:</strong>
                  <span
                    className={`badge ${
                      selectedEvidence.status === "effective"
                        ? "badge-success"
                        : selectedEvidence.status === "revoked"
                          ? "badge-error"
                          : "badge-warning"
                    } ml-2`}
                  >
                    {selectedEvidence.status === "effective"
                      ? "有效"
                      : selectedEvidence.status === "revoked"
                        ? "已撤销"
                        : "已过期"}
                  </span>
                </div>
                <div>
                  <strong>哈希算法:</strong> {selectedEvidence.hash.algorithm}
                </div>
                <div>
                  <strong>哈希值:</strong>{" "}
                  <span className="font-mono text-xs break-all">{selectedEvidence.hash.value}</span>
                </div>
              </div>
            </div>

            {selectedEvidence.memo && (
              <div className="mt-4">
                <strong>备注:</strong>
                <p className="mt-1 p-2 bg-base-200 rounded">{selectedEvidence.memo}</p>
              </div>
            )}

            <div className="modal-action">
              <button className="btn" onClick={() => setSelectedEvidence(null)}>
                关闭
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default History;
