import React from "react";
import Link from "next/link";
import { hardhat } from "viem/chains";
import { CurrencyDollarIcon, MagnifyingGlassIcon, SparklesIcon } from "@heroicons/react/24/outline";
import { AdaptiveBackground } from "~~/components/AdaptiveBackground";
import { AdaptiveText } from "~~/components/AdaptiveText";
import { Faucet } from "~~/components/scaffold-eth";
import { useAuth } from "~~/contexts/AuthContext";
import { useTargetNetwork } from "~~/hooks/scaffold-eth/useTargetNetwork";
import { useGlobalState } from "~~/services/store/store";

/**
 * Site footer - 现代玻璃态设计
 */
export const Footer = () => {
  const nativeCurrencyPrice = useGlobalState(state => state.nativeCurrency.price);
  const { targetNetwork } = useTargetNetwork();
  const isLocalNetwork = targetNetwork.id === hardhat.id;
  const { user } = useAuth();

  // 如果用户未登录，不渲染整个footer
  if (!user) {
    return null;
  }

  return (
    <>
      {/* 主要底部工具栏 */}
      <div className="fixed bottom-0 left-0 right-0 z-40">
        {/* 背景模糊效果 */}
        <AdaptiveBackground variant="footer" className="absolute inset-0">
          <div className="h-full" />
        </AdaptiveBackground>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14">
            {/* 左侧工具 */}
            <div className="flex items-center space-x-3">
              {/* 价格显示 */}
              {nativeCurrencyPrice > 0 && (
                <div className="hidden sm:flex items-center px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
                  <CurrencyDollarIcon className="w-4 h-4 text-green-400 mr-2" />
                  <AdaptiveText variant="secondary" className="text-sm">
                    {nativeCurrencyPrice.toFixed(2)}
                  </AdaptiveText>
                </div>
              )}

              {/* 本地网络工具 */}
              {isLocalNetwork && (
                <div className="hidden md:flex items-center space-x-2">
                  <Faucet />
                  <Link
                    href="/blockexplorer"
                    className="flex items-center px-3 py-1.5 rounded-lg text-sm hover:bg-white/5 border border-white/10 transition-all duration-200"
                  >
                    <AdaptiveText variant="secondary" className="flex items-center">
                      <MagnifyingGlassIcon className="w-4 h-4 mr-2" />
                      <span>Block Explorer</span>
                    </AdaptiveText>
                  </Link>
                </div>
              )}
            </div>

            {/* 右侧工具 */}
            <div className="flex items-center space-x-3">
              {/* 快速操作 */}
              {user && (
                <div className="hidden lg:flex items-center space-x-2">
                  <Link
                    href="/upload"
                    className="flex items-center px-3 py-1.5 rounded-lg text-sm hover:bg-blue-500/10 border border-blue-500/20 transition-all duration-200"
                  >
                    <AdaptiveText variant="secondary" className="flex items-center text-blue-400">
                      <SparklesIcon className="w-4 h-4 mr-2" />
                      <span>快速存证</span>
                    </AdaptiveText>
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
      {/* 版权信息区域 */}
      <div className="fixed bottom-14 left-0 right-0 z-30 pb-4">
        <div className="text-center">
          <p className="text-xs text-gray-500">
            © {new Date().getFullYear()} EvidenceChain 区块链存证系统.
            <span className="mx-2">|</span>
            <span className="text-gray-600">Built with ❤️ using Blockchain Technology</span>
          </p>
        </div>
      </div>
      {/* 为固定底部留出空间 */}
      <div className="h-28" /> {/* 为 footer 和版权信息留出空间 */}
    </>
  );
};
