import { useEffect, useState } from "react";
import { MutateOptions } from "@tanstack/react-query";
import { Abi, ExtractAbiFunctionNames } from "abitype";
import { Config, UseWriteContractParameters, useAccount, useConfig, useWriteContract } from "wagmi";
import { WriteContractErrorType, WriteContractReturnType } from "wagmi/actions";
import { WriteContractVariables } from "wagmi/query";
import { useSelectedNetwork } from "~~/hooks/scaffold-eth";
import { useDeployedContractInfo, useTransactor } from "~~/hooks/scaffold-eth";
import { AllowedChainIds, notification } from "~~/utils/scaffold-eth";
import {
  ContractAbi,
  ContractName,
  ScaffoldWriteContractOptions,
  ScaffoldWriteContractVariables,
  UseScaffoldWriteConfig,
  detectWalletType,
  getWalletTypeDescription,
  simulateContractWriteAndNotifyError,
} from "~~/utils/scaffold-eth/contract";

type ScaffoldWriteContractReturnType<TContractName extends ContractName> = Omit<
  ReturnType<typeof useWriteContract>,
  "writeContract" | "writeContractAsync"
> & {
  isMining: boolean;
  writeContractAsync: <
    TFunctionName extends ExtractAbiFunctionNames<ContractAbi<TContractName>, "nonpayable" | "payable">,
  >(
    variables: ScaffoldWriteContractVariables<TContractName, TFunctionName>,
    options?: ScaffoldWriteContractOptions,
  ) => Promise<WriteContractReturnType | undefined>;
  writeContract: <TFunctionName extends ExtractAbiFunctionNames<ContractAbi<TContractName>, "nonpayable" | "payable">>(
    variables: ScaffoldWriteContractVariables<TContractName, TFunctionName>,
    options?: Omit<ScaffoldWriteContractOptions, "onBlockConfirmation" | "blockConfirmations">,
  ) => void;
};

export function useScaffoldWriteContract<TContractName extends ContractName>(
  config: UseScaffoldWriteConfig<TContractName>,
): ScaffoldWriteContractReturnType<TContractName>;
/**
 * @deprecated Use object parameter version instead: useScaffoldWriteContract({ contractName: "YourContract" })
 */
export function useScaffoldWriteContract<TContractName extends ContractName>(
  contractName: TContractName,
  writeContractParams?: UseWriteContractParameters,
): ScaffoldWriteContractReturnType<TContractName>;

/**
 * Wrapper around wagmi's useWriteContract hook which automatically loads (by name) the contract ABI and address from
 * the contracts present in deployedContracts.ts & externalContracts.ts corresponding to targetNetworks configured in scaffold.config.ts
 * @param contractName - name of the contract to be written to
 * @param config.chainId - optional chainId that is configured with the scaffold project to make use for multi-chain interactions.
 * @param writeContractParams - wagmi's useWriteContract parameters
 */
export function useScaffoldWriteContract<TContractName extends ContractName>(
  configOrName: UseScaffoldWriteConfig<TContractName> | TContractName,
  writeContractParams?: UseWriteContractParameters,
): ScaffoldWriteContractReturnType<TContractName> {
  const finalConfig =
    typeof configOrName === "string"
      ? { contractName: configOrName, writeContractParams, chainId: undefined }
      : (configOrName as UseScaffoldWriteConfig<TContractName>);
  const { contractName, chainId, writeContractParams: finalWriteContractParams } = finalConfig;

  const wagmiConfig = useConfig();

  useEffect(() => {
    if (typeof configOrName === "string") {
      console.warn(
        "Using `useScaffoldWriteContract` with a string parameter is deprecated. Please use the object parameter version instead.",
      );
    }
  }, [configOrName]);

  const { chain: accountChain } = useAccount();
  const writeTx = useTransactor();
  const [isMining, setIsMining] = useState(false);

  const wagmiContractWrite = useWriteContract(finalWriteContractParams);

  const selectedNetwork = useSelectedNetwork(chainId);

  const { data: deployedContractData } = useDeployedContractInfo({
    contractName,
    chainId: selectedNetwork.id as AllowedChainIds,
  });

  const sendContractWriteAsyncTx = async <
    TFunctionName extends ExtractAbiFunctionNames<ContractAbi<TContractName>, "nonpayable" | "payable">,
  >(
    variables: ScaffoldWriteContractVariables<TContractName, TFunctionName>,
    options?: ScaffoldWriteContractOptions,
  ) => {
    if (!deployedContractData) {
      notification.error("Target Contract is not deployed, did you forget to run `yarn deploy`?");
      return;
    }

    if (!accountChain?.id) {
      notification.error("Please connect your wallet");
      return;
    }

    if (accountChain?.id !== selectedNetwork.id) {
      notification.error(`Wallet is connected to the wrong network. Please switch to ${selectedNetwork.name}`);
      return;
    }

    try {
      setIsMining(true);
      const totalStartTime = Date.now();
      console.log("🔧 [useScaffoldWriteContract] 开始合约写入流程");
      const { blockConfirmations, onBlockConfirmation, ...mutateOptions } = options || {};

      const writeContractObject = {
        abi: deployedContractData.abi as Abi,
        address: deployedContractData.address,
        ...variables,
      } as WriteContractVariables<Abi, string, any[], Config, number>;

      let simulateTime = 0;
      if (!finalConfig?.disableSimulate) {
        console.log("🔍 [useScaffoldWriteContract] 开始模拟调用...");
        const simulateStartTime = Date.now();
        await simulateContractWriteAndNotifyError({
          wagmiConfig,
          writeContractParams: writeContractObject,
          chainId: selectedNetwork.id as AllowedChainIds,
        });
        const simulateEndTime = Date.now();
        simulateTime = simulateEndTime - simulateStartTime;
        console.log(`✅ [useScaffoldWriteContract] 模拟调用完成, 耗时: ${simulateTime}ms`);
      } else {
        console.log("⏭️  [useScaffoldWriteContract] 模拟调用已禁用");
      }

      console.log("💰 [useScaffoldWriteContract] 准备实际交易...");
      const transactionStartTime = Date.now();

      const makeWriteWithParams = async () => {
        console.log("🔗 [useScaffoldWriteContract] 调用 wagmi writeContractAsync...");
        const wagmiCallStartTime = Date.now();
        const result = await wagmiContractWrite.writeContractAsync(
          writeContractObject,
          mutateOptions as
            | MutateOptions<
                WriteContractReturnType,
                WriteContractErrorType,
                WriteContractVariables<Abi, string, any[], Config, number>,
                unknown
              >
            | undefined,
        );
        const wagmiCallEndTime = Date.now();
        const wagmiTime = wagmiCallEndTime - wagmiCallStartTime;
        console.log(`✅ [useScaffoldWriteContract] wagmi调用完成, 耗时: ${wagmiTime}ms`);

        // 检测是否使用 MetaMask（wagmi调用时间较长通常表示外部钱包）
        if (wagmiTime > 1000) {
          console.log("⚠️  检测到可能的外部钱包（如MetaMask）连接，交易确认需要用户交互");
        }

        return result;
      };

      console.log("🚀 [useScaffoldWriteContract] 准备调用 useTransactor...");
      const transactorCallStartTime = Date.now();
      const writeTxResult = await writeTx(makeWriteWithParams, { blockConfirmations, onBlockConfirmation });
      const transactorCallEndTime = Date.now();
      const transactorTime = transactorCallEndTime - transactorCallStartTime;
      console.log(`✅ [useScaffoldWriteContract] useTransactor调用完成, 耗时: ${transactorTime}ms`);

      const transactionEndTime = Date.now();
      const totalTransactionTime = transactionEndTime - transactionStartTime;
      const totalTime = transactionEndTime - totalStartTime;

      console.log(`✅ [useScaffoldWriteContract] 交易执行耗时: ${totalTransactionTime}ms`);

      // 检测钱包类型并显示性能统计
      const walletType = detectWalletType(totalTransactionTime);
      const walletDescription = getWalletTypeDescription(walletType);
      console.log(`📊 [useScaffoldWriteContract] 总耗时: ${totalTime}ms (模拟: ${simulateTime}ms)`);
      console.log(`👛 [useScaffoldWriteContract] 检测到钱包类型: ${walletDescription}`);

      // 性能警告：如果总时间超过预期阈值
      if (totalTransactionTime > 30000) {
        // 30秒
        console.warn("⚠️  交易执行时间过长，请检查网络连接和钱包配置");
        console.log("💡 提示: 使用 Local Burner Wallet 可以获得更快的开发体验");
      }

      return writeTxResult;
    } catch (e: any) {
      console.error("❌ [useScaffoldWriteContract] 交易执行失败:", e);
      throw e;
    } finally {
      setIsMining(false);
    }
  };

  const sendContractWriteTx = <
    TContractName extends ContractName,
    TFunctionName extends ExtractAbiFunctionNames<ContractAbi<TContractName>, "nonpayable" | "payable">,
  >(
    variables: ScaffoldWriteContractVariables<TContractName, TFunctionName>,
    options?: Omit<ScaffoldWriteContractOptions, "onBlockConfirmation" | "blockConfirmations">,
  ) => {
    if (!deployedContractData) {
      notification.error("Target Contract is not deployed, did you forget to run `yarn deploy`?");
      return;
    }
    if (!accountChain?.id) {
      notification.error("Please connect your wallet");
      return;
    }

    if (accountChain?.id !== selectedNetwork.id) {
      notification.error(`Wallet is connected to the wrong network. Please switch to ${selectedNetwork.name}`);
      return;
    }

    wagmiContractWrite.writeContract(
      {
        abi: deployedContractData.abi as Abi,
        address: deployedContractData.address,
        ...variables,
      } as WriteContractVariables<Abi, string, any[], Config, number>,
      options as
        | MutateOptions<
            WriteContractReturnType,
            WriteContractErrorType,
            WriteContractVariables<Abi, string, any[], Config, number>,
            unknown
          >
        | undefined,
    );
  };

  return {
    ...wagmiContractWrite,
    isMining,
    // Overwrite wagmi's writeContactAsync
    writeContractAsync: sendContractWriteAsyncTx,
    // Overwrite wagmi's writeContract
    writeContract: sendContractWriteTx,
  };
}
