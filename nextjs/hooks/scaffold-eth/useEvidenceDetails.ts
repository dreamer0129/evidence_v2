import { useMemo } from "react";
import { useScaffoldReadContract } from "./useScaffoldReadContract";

export interface EvidenceDetails {
  evidenceId: string;
  userId: string;
  metadata: {
    fileName: string;
    mimeType: string;
    size: bigint;
    creationTime: bigint;
  };
  hash: {
    algorithm: string;
    value: string;
  };
  timestamp: bigint;
  blockHeight: bigint;
  status: string;
  memo: string;
  exists: boolean;
}

export const useEvidenceDetails = (evidenceId: string | undefined) => {
  const {
    data: evidenceData,
    isLoading,
    error,
  } = useScaffoldReadContract({
    contractName: "EvidenceStorageContract",
    functionName: "getEvidence",
    args: evidenceId ? [evidenceId] : ([] as any),
    query: {
      enabled: !!evidenceId,
    },
  });

  const evidenceDetails = useMemo(() => {
    if (!evidenceData) return null;

    // Convert the raw contract data to our typed format
    const evidence = evidenceData as any;
    return {
      evidenceId: evidence.evidenceId,
      userId: evidence.userId,
      metadata: {
        fileName: evidence.metadata.fileName,
        mimeType: evidence.metadata.mimeType,
        size: evidence.metadata.size,
        creationTime: evidence.metadata.creationTime,
      },
      hash: {
        algorithm: evidence.hash.algorithm,
        value: evidence.hash.value,
      },
      timestamp: evidence.timestamp,
      blockHeight: evidence.blockHeight,
      status: evidence.status,
      memo: evidence.memo,
      exists: evidence.exists,
    } as EvidenceDetails;
  }, [evidenceData]);

  return {
    data: evidenceDetails,
    isLoading,
    error,
  };
};
