import { TransactionHash } from "./TransactionHash";
import { formatEther } from "viem";
import { Address } from "~~/components/scaffold-eth";
import { useTargetNetwork } from "~~/hooks/scaffold-eth/useTargetNetwork";
import { TransactionWithFunction } from "~~/utils/scaffold-eth";
import { TransactionsTableProps } from "~~/utils/scaffold-eth/";

export const TransactionsTable = ({ blocks, transactionReceipts }: TransactionsTableProps) => {
  const { targetNetwork } = useTargetNetwork();

  return (
    <div className="flex justify-center px-4 md:px-0">
      <div className="overflow-x-auto w-full shadow-2xl rounded-xl">
        <table className="table text-xl bg-base-100 table-zebra w-full md:table-md table-sm">
          <thead>
            <tr className="rounded-xl text-sm font-semibold">
              <th className="bg-blue-600 text-white px-4 py-3 text-left">Transaction Hash</th>
              <th className="bg-blue-600 text-white px-4 py-3 text-left">Function Called</th>
              <th className="bg-blue-600 text-white px-4 py-3 text-left">Block Number</th>
              <th className="bg-blue-600 text-white px-4 py-3 text-left">Time Mined</th>
              <th className="bg-blue-600 text-white px-4 py-3 text-left">From</th>
              <th className="bg-blue-600 text-white px-4 py-3 text-left">To</th>
              <th className="bg-blue-600 text-white px-4 py-3 text-right">Value ({targetNetwork.nativeCurrency.symbol})</th>
            </tr>
          </thead>
          <tbody>
            {blocks.map(block =>
              (block.transactions as TransactionWithFunction[]).map(tx => {
                const receipt = transactionReceipts[tx.hash];
                const timeMined = new Date(Number(block.timestamp) * 1000).toLocaleString();
                const functionCalled = tx.input.substring(0, 10);

                return (
                  <tr key={tx.hash} className="hover:bg-gray-50 dark:hover:bg-gray-800 border-b border-gray-200 dark:border-gray-700 text-sm">
                    <td className="w-1/12 px-4 py-4">
                      <TransactionHash hash={tx.hash} />
                    </td>
                    <td className="w-2/12 px-4 py-4">
                      {tx.functionName === "0x" ? "" : <span className="mr-1 font-medium text-gray-900 dark:text-gray-100">{tx.functionName}</span>}
                      {functionCalled !== "0x" && (
                        <span className="badge bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200 font-bold text-xs px-2 py-1 rounded">{functionCalled}</span>
                      )}
                    </td>
                    <td className="w-1/12 px-4 py-4 font-mono text-gray-900 dark:text-gray-100">{block.number?.toString()}</td>
                    <td className="w-2/12 px-4 py-4 text-gray-900 dark:text-gray-100">{timeMined}</td>
                    <td className="w-2/12 px-4 py-4">
                      <Address address={tx.from} size="sm" onlyEnsOrAddress />
                    </td>
                    <td className="w-2/12 px-4 py-4">
                      {!receipt?.contractAddress ? (
                        tx.to && <Address address={tx.to} size="sm" onlyEnsOrAddress />
                      ) : (
                        <div className="relative">
                          <Address address={receipt.contractAddress} size="sm" onlyEnsOrAddress />
                          <small className="absolute top-4 left-4 text-gray-600 dark:text-gray-400">(Contract Creation)</small>
                        </div>
                      )}
                    </td>
                    <td className="text-right px-4 py-4 font-mono text-gray-900 dark:text-gray-100">
                      {formatEther(tx.value)} {targetNetwork.nativeCurrency.symbol}
                    </td>
                  </tr>
                );
              }),
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
