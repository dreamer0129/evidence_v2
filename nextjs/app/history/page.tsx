"use client";

import type { NextPage } from "next";
import { useAccount } from "wagmi";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "~~/components/ui/table";
import { useScaffoldReadContract } from "~~/hooks/scaffold-eth";

const History: NextPage = () => {
  const { address } = useAccount();

  // Fetch user's evidence IDs
  const { data: evidenceIds, isLoading: loadingIds } = useScaffoldReadContract({
    contractName: "EvidenceStorage",
    functionName: "getUserEvidences",
    args: [address],
  });

  // For demonstration, we'll show the evidence IDs - in a real app,
  // you would implement a batch request pattern or server-side API
  // to fetch detailed evidence information for each ID

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
                <TableHead></TableHead>
                <TableHead>证据ID</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {evidenceIds.map((evidenceId: string, index: number) => (
                <TableRow key={evidenceId}>
                  <TableCell>{index + 1}</TableCell>
                  <TableCell className="font-mono">{evidenceId}</TableCell>
                  <TableCell>
                    <button className="btn btn-sm btn-primary">查看详情</button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <div className="mt-4 text-sm text-gray-500 text-center">共 {evidenceIds.length} 条存证记录</div>
        </div>
      )}
    </div>
  );
};

export default History;
