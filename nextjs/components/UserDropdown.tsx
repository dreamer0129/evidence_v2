"use client";

import React, { useRef, useState } from "react";
import { useConnectModal } from "@rainbow-me/rainbowkit";
import { createWalletClient, http, parseEther } from "viem";
import { hardhat } from "viem/chains";
import { useAccount, useDisconnect } from "wagmi";
import {
  ArrowLeftStartOnRectangleIcon,
  BanknotesIcon,
  BellIcon,
  ChevronDownIcon,
  Cog6ToothIcon,
  DocumentDuplicateIcon,
  EyeIcon,
  QrCodeIcon,
  UserIcon,
} from "@heroicons/react/24/outline";
import { Balance, BlockieAvatar } from "~~/components/scaffold-eth";
import { AddressQRCodeModal } from "~~/components/scaffold-eth/RainbowKitCustomConnectButton/AddressQRCodeModal";
import { Button } from "~~/components/ui/button";
import { useCopyToClipboard, useOutsideClick, useTransactor } from "~~/hooks/scaffold-eth";
import { useTargetNetwork } from "~~/hooks/scaffold-eth/useTargetNetwork";
import { getBlockExplorerAddressLink } from "~~/utils/scaffold-eth";

interface UserDropdownProps {
  username: string;
  onLogout: () => void;
}

export const UserDropdown = ({ username, onLogout }: UserDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Wallet hooks
  const { address, isConnected } = useAccount();
  const { disconnect } = useDisconnect();
  const { openConnectModal } = useConnectModal();
  const { targetNetwork } = useTargetNetwork();

  // 创建本地钱包客户端和 transactor
  const localWalletClient = createWalletClient({
    chain: hardhat,
    transport: http(),
  });
  const faucetTxn = useTransactor(localWalletClient);

  const { copyToClipboard: copyAddressToClipboard, isCopiedToClipboard: isAddressCopiedToClipboard } =
    useCopyToClipboard();

  const blockExplorerAddressLink = address ? getBlockExplorerAddressLink(targetNetwork, address) : undefined;

  const isLocalNetwork = targetNetwork.id === hardhat.id;

  useOutsideClick(dropdownRef, () => {
    setIsOpen(false);
  });

  // 处理钱包连接
  const handleConnectWallet = () => {
    if (openConnectModal) {
      openConnectModal();
      setIsOpen(false);
    }
  };

  // 处理断开钱包连接
  const handleDisconnectWallet = () => {
    disconnect();
    setIsOpen(false);
  };

  return (
    <div className="relative" ref={dropdownRef}>
      {/* 用户头像和下拉触发器 */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-2 bg-white/5 hover:bg-white/10 border border-white/10 px-3 py-2"
      >
        {isConnected && address ? (
          <BlockieAvatar address={address} size={24} />
        ) : (
          <div className="w-6 h-6 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center">
            <UserIcon className="w-3 h-3 text-white" />
          </div>
        )}
        <span className="text-sm text-gray-900 max-w-20 truncate hidden sm:block">
          {isConnected && address ? `${address.slice(0, 6)}...${address.slice(-4)}` : username}
        </span>
        <ChevronDownIcon
          className={`w-4 h-4 text-gray-400 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`}
        />
      </Button>

      {/* 下拉菜单 */}
      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-72 bg-black/90 backdrop-blur-md border border-white/20 rounded-lg shadow-xl z-50">
          {/* 用户信息头部 */}
          <div className="p-4 border-b border-white/10">
            <div className="flex items-center space-x-3">
              {isConnected && address ? (
                <BlockieAvatar address={address} size={40} />
              ) : (
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center">
                  <UserIcon className="w-5 h-5 text-white" />
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">
                  {isConnected && address ? `${address.slice(0, 6)}...${address.slice(-4)}` : username}
                </p>
                <p className="text-xs text-gray-400">
                  {isConnected ? (
                    <span className="flex items-center space-x-1">
                      <span>{targetNetwork.name}</span>
                      <span>•</span>
                      <Balance address={address} className="text-xs text-gray-400" />
                    </span>
                  ) : (
                    "已登录用户"
                  )}
                </p>
              </div>
            </div>
          </div>

          {/* 菜单选项 */}
          <div className="p-2 space-y-1">
            {isConnected && address ? (
              <>
                {/* 钱包地址操作 */}
                <div className="px-3 py-2">
                  <div className="text-xs text-gray-400 mb-2">钱包地址</div>
                  <div className="space-y-1">
                    <button
                      className="w-full flex items-center justify-between px-2 py-1.5 text-sm text-gray-300 hover:text-white hover:bg-white/5 rounded-lg transition-colors"
                      onClick={() => copyAddressToClipboard(address)}
                    >
                      <span className="flex items-center space-x-2">
                        <DocumentDuplicateIcon className="w-4 h-4" />
                        <span>复制地址</span>
                      </span>
                      {isAddressCopiedToClipboard && <span className="text-xs text-green-400">已复制</span>}
                    </button>

                    <button
                      className="w-full flex items-center space-x-2 px-2 py-1.5 text-sm text-gray-300 hover:text-white hover:bg-white/5 rounded-lg transition-colors"
                      onClick={() => {
                        // 通过程序控制 checkbox
                        const checkbox = document.getElementById("address-qrcode-modal") as HTMLInputElement;
                        if (checkbox) {
                          checkbox.checked = true;
                        }
                        setIsOpen(false);
                      }}
                    >
                      <QrCodeIcon className="w-4 h-4" />
                      <span>显示二维码</span>
                    </button>

                    {blockExplorerAddressLink && (
                      <a
                        href={blockExplorerAddressLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center space-x-2 px-2 py-1.5 text-sm text-gray-300 hover:text-white hover:bg-white/5 rounded-lg transition-colors"
                      >
                        <EyeIcon className="w-4 h-4" />
                        <span>在区块浏览器中查看</span>
                      </a>
                    )}

                    {isLocalNetwork && (
                      <button
                        className="w-full flex items-center space-x-2 px-2 py-1.5 text-sm text-gray-300 hover:text-white hover:bg-white/5 rounded-lg transition-colors"
                        onClick={async () => {
                          // 复制 FaucetButton 的功能
                          if (!address) return;
                          try {
                            const NUM_OF_ETH = "1";
                            const FAUCET_ADDRESS = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

                            await faucetTxn({
                              account: FAUCET_ADDRESS,
                              to: address,
                              value: parseEther(NUM_OF_ETH),
                            });
                          } catch (error) {
                            console.error("Faucet error:", error);
                          }
                        }}
                      >
                        <BanknotesIcon className="w-4 h-4" />
                        <span>领取测试币</span>
                      </button>
                    )}
                  </div>
                </div>

                <div className="border-t border-white/10 my-2" />
              </>
            ) : (
              <button
                className="w-full flex items-center justify-center px-3 py-2 text-sm text-blue-400 hover:text-blue-300 hover:bg-blue-500/10 rounded-lg transition-colors"
                onClick={handleConnectWallet}
              >
                连接钱包
              </button>
            )}

            {/* 用户设置 */}
            <button
              className="w-full flex items-center space-x-3 px-3 py-2 text-sm text-gray-300 hover:text-white hover:bg-white/5 rounded-lg transition-colors"
              onClick={() => setIsOpen(false)}
            >
              <Cog6ToothIcon className="w-4 h-4" />
              <span>账户设置</span>
            </button>

            {/* 通知 */}
            <button
              className="w-full flex items-center space-x-3 px-3 py-2 text-sm text-gray-300 hover:text-white hover:bg-white/5 rounded-lg transition-colors"
              onClick={() => setIsOpen(false)}
            >
              <BellIcon className="w-4 h-4" />
              <span>通知中心</span>
              <span className="ml-auto bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">3</span>
            </button>

            {isConnected && (
              <>
                <div className="border-t border-white/10 my-2" />
                <button
                  className="w-full flex items-center space-x-3 px-3 py-2 text-sm text-yellow-400 hover:text-yellow-300 hover:bg-yellow-500/10 rounded-lg transition-colors"
                  onClick={handleDisconnectWallet}
                >
                  <ArrowLeftStartOnRectangleIcon className="w-4 h-4" />
                  <span>断开钱包</span>
                </button>
              </>
            )}

            <div className="border-t border-white/10 my-2" />

            {/* 退出登录 */}
            <button
              className="w-full flex items-center space-x-3 px-3 py-2 text-sm text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg transition-colors"
              onClick={onLogout}
            >
              <ArrowLeftStartOnRectangleIcon className="w-4 h-4" />
              <span>退出登录</span>
            </button>
          </div>
        </div>
      )}

      {/* Address QR Code Modal */}
      {address && <AddressQRCodeModal address={address} modalId="address-qrcode-modal" />}
    </div>
  );
};
