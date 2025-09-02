"use client";

import React from "react";
import { useAccount, useDisconnect } from "wagmi";
import { RainbowKitCustomConnectButton } from "~~/components/scaffold-eth";
import { Button } from "~~/components/ui/button";

interface MobileWalletConnectionProps {
  className?: string;
}

export const MobileWalletConnection = ({ className = "" }: MobileWalletConnectionProps) => {
  const { address, isConnected } = useAccount();
  const { disconnect } = useDisconnect();

  if (!isConnected) {
    return (
      <div className={`flex justify-center ${className}`}>
        <RainbowKitCustomConnectButton />
      </div>
    );
  }

  // 格式化地址显示
  const formatAddress = (address: string) => {
    return `${address.slice(0, 6)}...${address.slice(-4)}`;
  };

  return (
    <div className={`space-y-3 ${className}`}>
      {/* 钱包地址 */}
      <div className="bg-white/5 rounded-lg p-3 border border-white/10">
        <div className="text-xs text-gray-400 mb-1">钱包地址</div>
        <div className="text-sm text-white font-mono">{address ? formatAddress(address) : "未知地址"}</div>
      </div>

      {/* 网络信息 */}
      <div className="bg-white/5 rounded-lg p-3 border border-white/10">
        <div className="text-xs text-gray-400 mb-1">网络</div>
        <div className="text-sm text-white">本地测试网络</div>
      </div>

      {/* 余额信息 */}
      <div className="bg-white/5 rounded-lg p-3 border border-white/10">
        <div className="text-xs text-gray-400 mb-1">余额</div>
        <div className="text-sm text-white">-- ETH</div>
      </div>

      {/* 断开连接按钮 */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => disconnect()}
        className="w-full border-red-500/20 text-red-400 hover:bg-red-500/10 hover:text-red-300"
      >
        断开连接
      </Button>
    </div>
  );
};
