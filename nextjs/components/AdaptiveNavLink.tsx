"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTheme } from "next-themes";

interface AdaptiveNavLinkProps {
  href: string;
  icon: React.ReactNode;
  label: string;
}

export const AdaptiveNavLink = ({ href, icon, label }: AdaptiveNavLinkProps) => {
  const pathname = usePathname();
  const { theme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return (
      <Link href={href} className="flex items-center px-4 py-2 rounded-lg text-sm font-medium transition-colors">
        {icon}
        <span className="ml-3">{label}</span>
      </Link>
    );
  }

  const isDark = theme === "dark";
  const isActive = pathname === href;

  // 深色主题样式
  const darkActiveClasses = "bg-white/10 text-white border border-white/20";
  const darkInactiveClasses = "text-gray-300 hover:text-white hover:bg-white/5";

  // 浅色主题样式
  const lightActiveClasses = "bg-blue-100 text-blue-700 border border-blue-200";
  const lightInactiveClasses = "text-gray-600 hover:text-gray-800 hover:bg-gray-100 border border-transparent";

  const activeClasses = isDark ? darkActiveClasses : lightActiveClasses;
  const inactiveClasses = isDark ? darkInactiveClasses : lightInactiveClasses;

  return (
    <Link
      href={href}
      className={`flex items-center px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
        isActive ? activeClasses : inactiveClasses
      }`}
    >
      {icon}
      <span className="ml-3">{label}</span>
    </Link>
  );
};