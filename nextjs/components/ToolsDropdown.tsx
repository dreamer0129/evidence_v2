"use client";

import React, { useRef, useState } from "react";
import Link from "next/link";
import { BugAntIcon, ChevronDownIcon, MagnifyingGlassIcon, WrenchScrewdriverIcon } from "@heroicons/react/24/outline";
import { Button } from "~~/components/ui/button";
import { useOutsideClick } from "~~/hooks/scaffold-eth";
import { useTargetNetwork } from "~~/hooks/scaffold-eth/useTargetNetwork";
import { hardhat } from "viem/chains";
import { BanknotesIcon } from "@heroicons/react/24/outline";
import { Address as AddressType, createWalletClient, http, parseEther } from "viem";
import { useAccount } from "wagmi";
import { AddressInput, EtherInput } from "~~/components/scaffold-eth";
import { useTransactor } from "~~/hooks/scaffold-eth";
import { notification } from "~~/utils/scaffold-eth";

interface ToolsDropdownProps {
  className?: string;
}

export const ToolsDropdown = ({ className = "" }: ToolsDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isFaucetModalOpen, setIsFaucetModalOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { targetNetwork } = useTargetNetwork();
  const isLocalNetwork = targetNetwork.id === hardhat.id;

  useOutsideClick(dropdownRef, () => {
    setIsOpen(false);
  });

  return (
    <div className={`relative ${className}`} ref={dropdownRef}>
      {/* 工具下拉菜单触发器 */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-2 bg-white/5 hover:bg-white/10 border border-white/10 px-3 py-2"
      >
        <WrenchScrewdriverIcon className="w-4 h-4" />
        <span className="text-sm">工具</span>
        <ChevronDownIcon className={`w-4 h-4 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
      </Button>

      {/* 下拉菜单 */}
      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-56 bg-white/95 backdrop-blur-md border border-gray-200 rounded-lg shadow-xl z-50">
          {/* 网络工具 */}
          <div className="p-3">
            <div className="text-xs text-gray-500 mb-3">网络工具</div>
            <div className="space-y-2">
              {/* Faucet - 仅在本地网络显示 */}
              {isLocalNetwork && (
                <div 
                  className="flex items-center justify-between p-2 rounded hover:bg-gray-50 transition-colors cursor-pointer"
                  onClick={() => {
                    setIsFaucetModalOpen(true);
                    setIsOpen(false);
                  }}
                >
                  <div className="flex items-center space-x-2">
                    <BanknotesIcon className="w-4 h-4 text-gray-600" />
                    <span className="text-sm text-gray-700">Faucet</span>
                  </div>
                </div>
              )}
              
              {/* Block Explorer */}
              <Link
                href="/blockexplorer"
                className="flex items-center justify-between p-2 rounded hover:bg-gray-50 transition-colors"
                onClick={() => setIsOpen(false)}
              >
                <div className="flex items-center space-x-2">
                  <MagnifyingGlassIcon className="w-4 h-4 text-gray-600" />
                  <span className="text-sm text-gray-700">Block Explorer</span>
                </div>
              </Link>

              {/* Debug */}
              <Link
                href="/debug"
                className="flex items-center justify-between p-2 rounded hover:bg-gray-50 transition-colors"
                onClick={() => setIsOpen(false)}
              >
                <div className="flex items-center space-x-2">
                  <BugAntIcon className="w-4 h-4 text-gray-600" />
                  <span className="text-sm text-gray-700">Debug</span>
                </div>
              </Link>
            </div>
          </div>

          {/* 系统信息 */}
          <div className="p-3 border-t border-gray-100 bg-gray-50 rounded-b-lg">
            <div className="text-xs text-gray-500">
              <div>网络: {targetNetwork.name}</div>
              <div>链ID: {targetNetwork.id}</div>
            </div>
          </div>
        </div>
      )}

      {/* Faucet Modal */}
      {isFaucetModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-xl font-bold">Local Faucet</h3>
              <button 
                onClick={() => setIsFaucetModalOpen(false)}
                className="text-gray-500 hover:text-gray-700"
              >
                ✕
              </button>
            </div>
            <FaucetModalContent onClose={() => setIsFaucetModalOpen(false)} />
          </div>
        </div>
      )}
    </div>
  );
};

// Account index to use from generated hardhat accounts.
const FAUCET_ACCOUNT_INDEX = 0;

const localWalletClient = createWalletClient({
  chain: hardhat,
  transport: http(),
});

// Faucet modal content component
const FaucetModalContent = ({ onClose }: { onClose: () => void }) => {
  const [loading, setLoading] = useState(false);
  const [inputAddress, setInputAddress] = useState<AddressType>();
  const [faucetAddress, setFaucetAddress] = useState<AddressType>();
  const [sendValue, setSendValue] = useState("");

  const { chain: ConnectedChain } = useAccount();
  const faucetTxn = useTransactor(localWalletClient);

  React.useEffect(() => {
    const getFaucetAddress = async () => {
      try {
        const accounts = await localWalletClient.getAddresses();
        setFaucetAddress(accounts[FAUCET_ACCOUNT_INDEX]);
      } catch (error) {
        notification.error(
          <>
            <p className="font-bold mt-0 mb-1">Cannot connect to local provider</p>
            <p className="m-0">
              - Did you forget to run <code className="italic bg-base-300 text-base font-bold">yarn chain</code> ?
            </p>
            <p className="mt-1 break-normal">
              - Or you can change <code className="italic bg-base-300 text-base font-bold">targetNetwork</code> in{" "}
              <code className="italic bg-base-300 text-base font-bold">scaffold.config.ts</code>
            </p>
          </>,
        );
        console.error("⚡️ ~ file: ToolsDropdown.tsx:getFaucetAddress ~ error", error);
      }
    };
    getFaucetAddress();
  }, []);

  const sendETH = async () => {
    if (!faucetAddress || !inputAddress) {
      return;
    }
    try {
      setLoading(true);
      await faucetTxn({
        to: inputAddress,
        value: parseEther(sendValue as `${number}`),
        account: faucetAddress,
      });
      setLoading(false);
      setInputAddress(undefined);
      setSendValue("");
      onClose();
    } catch (error) {
      console.error("⚡️ ~ file: ToolsDropdown.tsx:sendETH ~ error", error);
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex space-x-4">
        <div>
          <span className="text-sm font-bold">From:</span>
          <div className="text-sm">{faucetAddress?.slice(0, 6)}...{faucetAddress?.slice(-4)}</div>
        </div>
        <div>
          <span className="text-sm font-bold pl-3">Available:</span>
          <div className="text-sm">Balance: 10000 ETH</div>
        </div>
      </div>
      
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Destination Address
        </label>
        <AddressInput
          placeholder="Destination Address"
          value={inputAddress ?? ""}
          onChange={value => setInputAddress(value as AddressType)}
        />
      </div>
      
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Amount to send
        </label>
        <EtherInput placeholder="Amount to send" value={sendValue} onChange={value => setSendValue(value)} />
      </div>
      
      <button
        onClick={sendETH}
        disabled={loading}
        className="w-full bg-blue-500 text-white py-2 px-4 rounded-md hover:bg-blue-600 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
      >
        {!loading ? (
          <>
            <BanknotesIcon className="h-4 w-4" />
            <span>Send</span>
          </>
        ) : (
          <>
            <span className="loading loading-spinner loading-sm"></span>
            <span>Sending...</span>
          </>
        )}
      </button>
    </div>
  );
};