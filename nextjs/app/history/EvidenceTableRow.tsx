"use client";

import { TableCell, TableRow } from "~~/components/ui/table";
import { type EvidenceDetails, useEvidenceDetails } from "~~/hooks/scaffold-eth/useEvidenceDetails";

interface EvidenceTableRowProps {
  evidenceId: string;
  index: number;
  onViewDetails: (evidence: EvidenceDetails) => void;
}

export const EvidenceTableRow = ({ evidenceId, index, onViewDetails }: EvidenceTableRowProps) => {
  const { data, isLoading } = useEvidenceDetails(evidenceId);

  return (
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
          Number(data.timestamp) > 1000000000000 
            ? new Date(Number(data.timestamp)).toLocaleString("zh-CN")
            : new Date(Number(data.timestamp) * 1000).toLocaleString("zh-CN")
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
          className="btn btn-sm btn-primary"
          onClick={() => data && onViewDetails(data)}
          disabled={isLoading || !data}
        >
          查看详情
        </button>
      </TableCell>
    </TableRow>
  );
};
