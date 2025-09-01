"use client";

import React, { useRef, useState } from "react";
import {
  ArrowLeftStartOnRectangleIcon,
  BellIcon,
  ChevronDownIcon,
  Cog6ToothIcon,
  UserIcon,
} from "@heroicons/react/24/outline";
import { Button } from "~~/components/ui/button";
import { useOutsideClick } from "~~/hooks/scaffold-eth";

interface UserDropdownProps {
  username: string;
  onLogout: () => void;
}

export const UserDropdown = ({ username, onLogout }: UserDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useOutsideClick(dropdownRef, () => {
    setIsOpen(false);
  });

  return (
    <div className="relative" ref={dropdownRef}>
      {/* 用户头像和下拉触发器 */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-2 bg-white/5 hover:bg-white/10 border border-white/10 px-3 py-2"
      >
        <div className="w-6 h-6 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center">
          <UserIcon className="w-3 h-3 text-white" />
        </div>
        <span className="text-sm text-gray-300 max-w-16 truncate hidden sm:block">{username}</span>
        <ChevronDownIcon className={`w-4 h-4 text-gray-400 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`} />
      </Button>

      {/* 下拉菜单 */}
      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-56 bg-black/90 backdrop-blur-md border border-white/20 rounded-lg shadow-xl z-50">
          {/* 用户信息头部 */}
          <div className="p-4 border-b border-white/10">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center">
                <UserIcon className="w-5 h-5 text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">{username}</p>
                <p className="text-xs text-gray-400">已登录用户</p>
              </div>
            </div>
          </div>

          {/* 菜单选项 */}
          <div className="p-2 space-y-1">
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

            {/* 分隔线 */}
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
    </div>
  );
};