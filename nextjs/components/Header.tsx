"use client";

import React, { useRef, useState } from "react";
import Link from "next/link";
import { hardhat } from "viem/chains";
import {
  ArrowUpTrayIcon,
  Bars3Icon,
  CheckBadgeIcon,
  ClockIcon,
  HomeIcon,
  MagnifyingGlassIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { AdaptiveBackground } from "~~/components/AdaptiveBackground";
import { AdaptiveNavLink } from "~~/components/AdaptiveNavLink";
import { AdaptiveText } from "~~/components/AdaptiveText";
import { MobileWalletConnection } from "~~/components/MobileWalletConnection";
import { ToolsDropdown } from "~~/components/ToolsDropdown";
import { UserDropdown } from "~~/components/UserDropdown";
import { FaucetButton } from "~~/components/scaffold-eth";
import { useAuth } from "~~/contexts/AuthContext";
import { useOutsideClick, useTargetNetwork } from "~~/hooks/scaffold-eth";

type HeaderMenuLink = {
  label: string;
  href: string;
  icon?: React.ReactNode;
};

export const menuLinks: HeaderMenuLink[] = [
  {
    label: "主页",
    href: "/",
    icon: <HomeIcon className="h-4 w-4" />,
  },
  {
    label: "立刻存证",
    href: "/upload",
    icon: <ArrowUpTrayIcon className="h-4 w-4" />,
  },
  {
    label: "核验中心",
    href: "/verify",
    icon: <CheckBadgeIcon className="h-4 w-4" />,
  },
  {
    label: "我的存证",
    href: "/history",
    icon: <ClockIcon className="h-4 w-4" />,
  },
];

export const HeaderMenuLinks = () => {
  return (
    <>
      {menuLinks.map(({ label, href, icon }) => (
        <li key={href} className="list-none">
          <AdaptiveNavLink href={href} icon={icon} label={label} />
        </li>
      ))}
    </>
  );
};

/**
 * Site header - 现代玻璃态设计
 */
export const Header = () => {
  const { targetNetwork } = useTargetNetwork();
  const isLocalNetwork = targetNetwork.id === hardhat.id;
  const { user, logout } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const mobileMenuRef = useRef<HTMLDivElement>(null);

  useOutsideClick(mobileMenuRef, () => {
    setIsMobileMenuOpen(false);
  });

  // 如果用户未登录，不渲染整个header
  if (!user) {
    return null;
  }

  return (
    <header className="fixed top-0 left-0 right-0 z-50">
      <AdaptiveBackground variant="header" className="absolute inset-0">
        <div className="h-full" />
      </AdaptiveBackground>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo and Brand */}
          <div className="flex items-center flex-shrink-0">
            <Link href="/" className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-500 rounded-lg flex items-center justify-center">
                <HomeIcon className="w-5 h-5 text-white" />
              </div>
              <div>
                <AdaptiveText variant="primary" className="font-bold text-lg">
                  EvidenceChain
                </AdaptiveText>
                <AdaptiveText variant="muted" className="text-xs block">
                  区块链存证系统
                </AdaptiveText>
              </div>
            </Link>
          </div>

          {/* Desktop Navigation - Center */}
          <div className="hidden md:flex flex-1 justify-center">
            <nav className="flex items-center space-x-1">
              <HeaderMenuLinks />
            </nav>
          </div>

          {/* Right side actions */}
          <div className="flex items-center space-x-2 flex-shrink-0">
            {/* Tools Dropdown */}
            <ToolsDropdown />

            {/* User Dropdown */}
            <UserDropdown username={user.username} onLogout={logout} />

            {/* Mobile menu button */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden p-2 rounded-lg transition-colors"
            >
              <AdaptiveText variant="secondary">
                {isMobileMenuOpen ? <XMarkIcon className="w-5 h-5" /> : <Bars3Icon className="w-5 h-5" />}
              </AdaptiveText>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {isMobileMenuOpen && (
        <div ref={mobileMenuRef} className="md:hidden absolute top-16 left-0 right-0">
          <AdaptiveBackground variant="header" className="absolute inset-0">
            <div className="h-full" />
          </AdaptiveBackground>
          <div className="px-4 py-2 space-y-1">
            <HeaderMenuLinks />

            {/* Mobile Tools */}
            <div className="border-t border-gray-200 pt-4 mt-4">
              <AdaptiveText variant="muted" className="text-xs mb-2">
                工具
              </AdaptiveText>
              <div className="space-y-2">
                {/* Mobile Faucet */}
                {isLocalNetwork && (
                  <div className="flex items-center justify-between">
                    <AdaptiveText variant="secondary" className="text-sm">
                      Faucet
                    </AdaptiveText>
                    <FaucetButton />
                  </div>
                )}

                {/* Mobile Block Explorer */}
                <Link
                  href="/blockexplorer"
                  className="flex items-center justify-between text-sm hover:bg-gray-100 rounded px-2 py-1 transition-colors"
                  onClick={() => setIsMobileMenuOpen(false)}
                >
                  <AdaptiveText variant="secondary" className="flex items-center">
                    <MagnifyingGlassIcon className="w-4 h-4 mr-2" />
                    <span>Block Explorer</span>
                  </AdaptiveText>
                </Link>
              </div>
            </div>

            {/* Mobile Wallet Connection */}
            <div className="border-t border-gray-200 pt-4 mt-4">
              <AdaptiveText variant="muted" className="text-xs mb-2">
                钱包连接
              </AdaptiveText>
              <MobileWalletConnection />
            </div>
          </div>
        </div>
      )}
    </header>
  );
};
