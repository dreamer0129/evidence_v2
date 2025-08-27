"use client";

import type { NextPage } from "next";

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
        <table className="table w-full">
          <thead>
            <tr>
              <th></th>
              <th>文件名称</th>
              <th>存证时间</th>
              <th>文件哈希</th>
              <th>交易哈希</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {mockData.map((item, index) => (
              <tr key={index}>
                <th>{index + 1}</th>
                <td>{item.fileName}</td>
                <td>{item.timestamp}</td>
                <td className="font-mono">{item.fileHash}</td>
                <td className="font-mono">{item.txHash}</td>
                <td>
                  <button className="btn btn-sm btn-primary">下载证书</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default History;
