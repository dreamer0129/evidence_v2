"use client";

import { useState } from "react";
import { EvidenceTableRow } from "./EvidenceTableRow";
import { BookOpen, FileText, Inbox, Loader2 } from "lucide-react";
import type { NextPage } from "next";
import { useAccount } from "wagmi";
import { GlassCard, GlassContainer } from "~~/components/evidence/GlassContainer";
import { PageBackgroundWrapper } from "~~/components/evidence/PageBackgroundWrapper";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "~~/components/ui/table";
import { useScaffoldReadContract } from "~~/hooks/scaffold-eth";
import { type EvidenceDetails } from "~~/hooks/scaffold-eth/useEvidenceDetails";

const History: NextPage = () => {
  const { address } = useAccount();
  const [selectedEvidence, setSelectedEvidence] = useState<EvidenceDetails | null>(null);

  // Fetch user's evidence IDs
  const { data: evidenceIds, isLoading: loadingIds } = useScaffoldReadContract({
    contractName: "EvidenceStorageContract",
    functionName: "getUserEvidences",
    args: [address],
  });

  return (
    <PageBackgroundWrapper>
      <div className="min-h-screen pt-24 pb-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-6xl mx-auto">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-black dark:text-white mb-2">我的存证记录</h1>
            <p className="text-base text-gray-800 dark:text-gray-300">管理和查看您的所有区块链存证记录</p>
          </div>

          {!address ? (
            <GlassCard className="text-center py-12">
              <Inbox className="w-16 h-16 mx-auto mb-4 text-gray-400" />
              <h3 className="text-xl font-semibold text-gray-700 dark:text-gray-200 mb-2">请连接钱包</h3>
              <p className="text-gray-500 dark:text-gray-400">连接钱包后查看您的存证记录</p>
            </GlassCard>
          ) : loadingIds ? (
            <GlassCard className="text-center py-12">
              <Loader2 className="w-16 h-16 mx-auto mb-4 animate-spin text-blue-500" />
              <h3 className="text-xl font-semibold text-gray-700 dark:text-gray-200 mb-2">加载中</h3>
              <p className="text-gray-500 dark:text-gray-400">正在加载您的存证记录...</p>
            </GlassCard>
          ) : (
            <>
              {!evidenceIds || evidenceIds.length === 0 ? (
                <GlassCard className="text-center py-12">
                  <FileText className="w-16 h-16 mx-auto mb-4 text-gray-400" />
                  <h3 className="text-xl font-semibold text-gray-700 dark:text-gray-200 mb-2">暂无存证记录</h3>
                  <p className="text-gray-500 dark:text-gray-400 mb-6">您还没有创建任何存证记录</p>
                  <button className="px-6 py-3 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors">
                    创建第一个存证
                  </button>
                </GlassCard>
              ) : (
                <GlassCard>
                  <div className="overflow-x-auto">
                    <Table>
                      <TableHeader>
                        <TableRow className="hover:bg-transparent">
                          <TableHead className="w-12 text-gray-700 dark:text-gray-300">序号</TableHead>
                          <TableHead className="text-gray-700 dark:text-gray-300">证据ID</TableHead>
                          <TableHead className="text-gray-700 dark:text-gray-300">文件名</TableHead>
                          <TableHead className="text-gray-700 dark:text-gray-300">文件大小</TableHead>
                          <TableHead className="text-gray-700 dark:text-gray-300">存证时间</TableHead>
                          <TableHead className="text-gray-700 dark:text-gray-300">文件类型</TableHead>
                          <TableHead className="text-gray-700 dark:text-gray-300">状态</TableHead>
                          <TableHead className="text-gray-700 dark:text-gray-300 text-right">操作</TableHead>
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
                  </div>
                  <div className="mt-4 text-center">
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      共 <span className="font-semibold">{evidenceIds.length}</span> 条存证记录
                    </p>
                  </div>
                </GlassCard>
              )}
            </>
          )}

          {/* Evidence Detail Modal */}
          {selectedEvidence && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
              <GlassCard className="max-w-4xl w-full max-h-[90vh] overflow-y-auto bg-white/95 dark:bg-gray-900/95 border border-gray-300/50 dark:border-gray-700/50">
                {/* Modal Header */}
                <div className="flex items-center justify-between mb-6">
                  <div className="flex items-center space-x-3">
                    <div className="p-2 bg-blue-500/20 rounded-lg">
                      <BookOpen className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                    </div>
                    <h3 className="text-xl font-semibold text-gray-900 dark:text-white font-bold">证据详情</h3>
                  </div>
                  <button
                    onClick={() => setSelectedEvidence(null)}
                    className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>

                {/* Modal Content */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Left Column */}
                  <div className="space-y-4">
                    <GlassContainer
                      intensity="low"
                      className="p-4 bg-gray-50/80 dark:bg-gray-800/80 border border-gray-200/50 dark:border-gray-700/50"
                    >
                      <div className="space-y-3">
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            证据ID
                          </label>
                          <p className="mt-1 font-mono text-sm text-gray-900 dark:text-white break-all font-semibold">
                            {selectedEvidence.evidenceId}
                          </p>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            文件名
                          </label>
                          <p className="mt-1 text-gray-900 dark:text-white font-medium">
                            {selectedEvidence.metadata.fileName || "-"}
                          </p>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            文件大小
                          </label>
                          <p className="mt-1 text-gray-900 dark:text-white font-medium">
                            {selectedEvidence.metadata.size > 0
                              ? `${(Number(selectedEvidence.metadata.size) / 1024).toFixed(1)} KB`
                              : "-"}
                          </p>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            文件类型
                          </label>
                          <p className="mt-1 text-gray-900 dark:text-white font-medium">
                            {selectedEvidence.metadata.mimeType || "-"}
                          </p>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            创建时间
                          </label>
                          <p className="mt-1 text-gray-900 dark:text-white font-medium">
                            {Number(selectedEvidence.metadata.creationTime) > 1000000000000
                              ? new Date(Number(selectedEvidence.metadata.creationTime)).toLocaleString("zh-CN")
                              : new Date(Number(selectedEvidence.metadata.creationTime) * 1000).toLocaleString("zh-CN")}
                          </p>
                        </div>
                      </div>
                    </GlassContainer>
                  </div>

                  {/* Right Column */}
                  <div className="space-y-4">
                    <GlassContainer
                      intensity="low"
                      className="p-4 bg-gray-50/80 dark:bg-gray-800/80 border border-gray-200/50 dark:border-gray-700/50"
                    >
                      <div className="space-y-3">
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            存证时间
                          </label>
                          <p className="mt-1 text-gray-900 dark:text-white font-medium">
                            {Number(selectedEvidence.timestamp) > 1000000000000
                              ? new Date(Number(selectedEvidence.timestamp)).toLocaleString("zh-CN")
                              : new Date(Number(selectedEvidence.timestamp) * 1000).toLocaleString("zh-CN")}
                          </p>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            区块高度
                          </label>
                          <p className="mt-1 font-mono text-gray-900 dark:text-white font-semibold">
                            #{selectedEvidence.blockHeight.toString()}
                          </p>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            状态
                          </label>
                          <div className="mt-1">
                            <span
                              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                selectedEvidence.status === "effective"
                                  ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                                  : selectedEvidence.status === "revoked"
                                    ? "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400"
                                    : "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400"
                              }`}
                            >
                              <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-current"></span>
                              {selectedEvidence.status === "effective"
                                ? "有效"
                                : selectedEvidence.status === "revoked"
                                  ? "已撤销"
                                  : "已过期"}
                            </span>
                          </div>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            哈希算法
                          </label>
                          <p className="mt-1 text-gray-900 dark:text-white font-medium">
                            {selectedEvidence.hash.algorithm}
                          </p>
                        </div>
                        <div>
                          <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">
                            哈希值
                          </label>
                          <p className="mt-1 font-mono text-xs text-gray-900 dark:text-white break-all font-semibold">
                            {selectedEvidence.hash.value}
                          </p>
                        </div>
                      </div>
                    </GlassContainer>
                  </div>
                </div>

                {/* Memo Section */}
                {selectedEvidence.memo && (
                  <GlassContainer
                    intensity="low"
                    className="p-4 bg-gray-50/80 dark:bg-gray-800/80 border border-gray-200/50 dark:border-gray-700/50"
                  >
                    <label className="text-sm font-medium text-gray-700 dark:text-gray-300 font-semibold">备注</label>
                    <p className="mt-2 text-gray-900 dark:text-white font-medium">{selectedEvidence.memo}</p>
                  </GlassContainer>
                )}

                {/* Modal Footer */}
                <div className="flex justify-end mt-6 pt-4 border-t border-gray-300 dark:border-gray-600">
                  <button
                    onClick={() => setSelectedEvidence(null)}
                    className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-lg transition-colors"
                  >
                    关闭
                  </button>
                </div>
              </GlassCard>
            </div>
          )}
        </div>
      </div>
    </PageBackgroundWrapper>
  );
};

export default History;
