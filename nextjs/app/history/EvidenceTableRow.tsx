"use client";

import { TableCell, TableRow } from "~~/components/ui/table";
import { type EvidenceDetails, useEvidenceDetails } from "~~/hooks/scaffold-eth/useEvidenceDetails";

interface EvidenceTableRowProps {
  evidenceId: string;
  index: number;
  onViewDetails: (evidence: EvidenceDetails) => void;
  onViewCertificate: (evidenceId: string, fileName?: string) => void;
}

export const EvidenceTableRow = ({ evidenceId, index, onViewDetails, onViewCertificate }: EvidenceTableRowProps) => {
  const { data, isLoading } = useEvidenceDetails(evidenceId);

  const handleDownloadCertificate = async (evidenceId?: string) => {
    if (!evidenceId) return;

    try {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("请先登录");
        return;
      }

      const response = await fetch(`http://localhost:8080/api/evidence/evidenceId/${evidenceId}/certificate/download`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `certificate_${evidenceId}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      } else if (response.status === 401) {
        alert("登录已过期，请重新登录");
        localStorage.removeItem("token");
        window.location.href = "/auth";
      } else if (response.status === 404) {
        alert("证书不存在");
      } else {
        alert("下载证书失败");
      }
    } catch (error) {
      console.error("Error downloading certificate:", error);
      alert("网络错误，请检查后端服务是否启动");
    }
  };

  return (
    <>
      <TableRow key={evidenceId}>
        <TableCell>{index + 1}</TableCell>
        <TableCell className="font-mono text-xs">{evidenceId}</TableCell>
        <TableCell>
          {isLoading ? (
            <div className="loading loading-spinner loading-xs"></div>
          ) : data ? (
            <div
              className="font-medium text-gray-900 dark:text-gray-100 truncate max-w-xs"
              title={data.metadata.fileName || "-"}
            >
              {data.metadata.fileName || "-"}
            </div>
          ) : (
            "-"
          )}
        </TableCell>
        <TableCell>
          {isLoading ? (
            <div className="loading loading-spinner loading-xs"></div>
          ) : data && data.metadata.size > 0 ? (
            `${(Number(data.metadata.size) / 1024).toFixed(1)} KB`
          ) : (
            "-"
          )}
        </TableCell>
        <TableCell>
          {isLoading ? (
            <div className="loading loading-spinner loading-xs"></div>
          ) : data ? (
            Number(data.timestamp) > 1000000000000 ? (
              new Date(Number(data.timestamp)).toLocaleString("zh-CN")
            ) : (
              new Date(Number(data.timestamp) * 1000).toLocaleString("zh-CN")
            )
          ) : (
            "-"
          )}
        </TableCell>
        <TableCell>
          {isLoading ? (
            <div className="loading loading-spinner loading-xs"></div>
          ) : data ? (
            data.metadata.mimeType || "-"
          ) : (
            "-"
          )}
        </TableCell>
        <TableCell>
          {isLoading ? (
            <div className="loading loading-spinner loading-xs"></div>
          ) : data ? (
            <span
              className={`badge ${
                data.status === "effective"
                  ? "badge-success"
                  : data.status === "revoked"
                    ? "badge-error"
                    : "badge-warning"
              }`}
            >
              {data.status === "effective" ? "有效" : data.status === "revoked" ? "已撤销" : "已过期"}
            </span>
          ) : (
            "-"
          )}
        </TableCell>
        <TableCell>
          <button
            className="btn btn-sm btn-primary mr-2"
            onClick={() => data && onViewDetails(data)}
            disabled={isLoading || !data}
          >
            查看详情
          </button>
          <button
            className="btn btn-sm btn-secondary mr-2"
            onClick={() => onViewCertificate(evidenceId, data?.metadata.fileName)}
            disabled={isLoading || !data}
          >
            查看证书
          </button>
          <button
            className="btn btn-sm btn-accent"
            onClick={() => handleDownloadCertificate(data?.evidenceId)}
            disabled={isLoading || !data}
          >
            下载证书
          </button>
        </TableCell>
      </TableRow>
    </>
  );
};