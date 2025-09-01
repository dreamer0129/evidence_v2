"use client";

import React, { useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { hardhat } from "viem/chains";
import {
  ArrowUpTrayIcon,
  Bars3Icon,
  BugAntIcon,
  CheckBadgeIcon,
  ClockIcon,
  DocumentTextIcon,
  HomeIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { MobileWalletConnection } from "~~/components/MobileWalletConnection";
import { UserDropdown } from "~~/components/UserDropdown";
import { FaucetButton, RainbowKitCustomConnectButton } from "~~/components/scaffold-eth";
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
    label: "存证",
    href: "/upload",
    icon: <ArrowUpTrayIcon className="h-4 w-4" />,
  },
  {
    label: "验证",
    href: "/verify",
    icon: <CheckBadgeIcon className="h-4 w-4" />,
  },
  {
    label: "历史",
    href: "/history",
    icon: <ClockIcon className="h-4 w-4" />,
  },
  {
    label: "文档",
    href: "/docs",
    icon: <DocumentTextIcon className="h-4 w-4" />,
  },
  {
    label: "Debug",
    href: "/debug",
    icon: <BugAntIcon className="h-4 w-4" />,
  },
];

export const HeaderMenuLinks = () => {
  const pathname = usePathname();

  return (
    <>
      {menuLinks.map(({ label, href, icon }) => {
        const isActive = pathname === href;
        return (
          <li key={href}>
            <Link
              href={href}
              className={`flex items-center px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200 ${
                isActive
                  ? "bg-white/10 text-white border border-white/20"
                  : "text-gray-300 hover:text-white hover:bg-white/5"
              }`}
            >
              {icon}
              <span className="ml-3">{label}</span>
            </Link>
          </li>
        );
      })}
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
      {/* 背景模糊效果 */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-md border-b border-white/10" />

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo and Brand */}
          <div className="flex items-center flex-shrink-0">
            <Link href="/" className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-500 rounded-lg flex items-center justify-center">
                <HomeIcon className="w-5 h-5 text-white" />
              </div>
              <div>
                <span className="text-white font-bold text-lg">EvidenceChain</span>
                <span className="text-xs text-gray-400 block">区块链存证系统</span>
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
            {/* User Dropdown */}
            <UserDropdown username={user.username} onLogout={logout} />

            {/* Wallet Connection */}
            <div className="hidden sm:block">
              <RainbowKitCustomConnectButton />
            </div>

            {/* Network Tools */}
            {isLocalNetwork && (
              <div className="hidden lg:block">
                <FaucetButton />
              </div>
            )}

            {/* Mobile menu button */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden p-2 rounded-lg text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
            >
              {isMobileMenuOpen ? <XMarkIcon className="w-5 h-5" /> : <Bars3Icon className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {isMobileMenuOpen && (
        <div
          ref={mobileMenuRef}
          className="md:hidden absolute top-16 left-0 right-0 bg-black/90 backdrop-blur-md border-t border-white/10"
        >
          <div className="px-4 py-2 space-y-1">
            <HeaderMenuLinks />

            {/* Mobile Wallet Connection */}
            <div className="border-t border-white/10 pt-4 mt-4">
              <div className="text-xs text-gray-400 mb-2">钱包连接</div>
              <MobileWalletConnection />
            </div>

            {/* Mobile Network Tools */}
            {isLocalNetwork && (
              <div className="border-t border-white/10 pt-4 mt-4">
                <div className="flex justify-center">
                  <FaucetButton />
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
};
