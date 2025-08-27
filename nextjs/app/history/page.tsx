"use client";

import type { NextPage } from "next";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "~~/components/ui/table";

const mockData = [
  {
    fileName: "合同-2023-A.pdf",
    timestamp: "2023-10-26 10:30:00",
    fileHash: "a1b2c3d4...e5f6",
    txHash: "0x1234...5678",
  },
  {
    fileName: "设计稿-v1.2.zip",
    timestamp: "2023-10-25 15:45:12",
    fileHash: "b2c3d4e5...f6a1",
    txHash: "0xabcd...efgh",
  },
  {
    fileName: "会议纪要.docx",
    timestamp: "2023-10-24 09:00:05",
    fileHash: "c3d4e5f6...a1b2",
    txHash: "0x5678...1234",
  },
];

const History: NextPage = () => {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-center mb-8">历史记录</h1>

      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead></TableHead>
              <TableHead>文件名称</TableHead>
              <TableHead>存证时间</TableHead>
              <TableHead>文件哈希</TableHead>
              <TableHead>交易哈希</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {mockData.map((item, index) => (
              <TableRow key={index}>
                <TableCell>{index + 1}</TableCell>
                <TableCell>{item.fileName}</TableCell>
                <TableCell>{item.timestamp}</TableCell>
                <TableCell className="font-mono">{item.fileHash}</TableCell>
                <TableCell className="font-mono">{item.txHash}</TableCell>
                <TableCell>
                  <button className="btn btn-sm btn-primary">下载证书</button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

export default History;
