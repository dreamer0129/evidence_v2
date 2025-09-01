"use client";

import React from "react";
import { useTheme } from "next-themes";

interface AdaptiveBackgroundProps {
  children: React.ReactNode;
  className?: string;
  variant?: "header" | "footer";
}

export const AdaptiveBackground = ({ children, className = "", variant = "header" }: AdaptiveBackgroundProps) => {
  const { theme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  // 避免服务器端渲染不匹配
  React.useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    // 默认返回浅色主题样式
    return <div className={className}>{children}</div>;
  }

  const isDark = theme === "dark";

  // 深色主题样式（适配主页）
  const darkClasses =
    variant === "header"
      ? "bg-black/40 backdrop-blur-md border-b border-white/10"
      : "bg-black/40 backdrop-blur-md border-t border-white/10";

  // 浅色主题样式（适配其他页面）
  const lightClasses =
    variant === "header"
      ? "bg-white/95 backdrop-blur-md border-b border-gray-200"
      : "bg-white/95 backdrop-blur-md border-t border-gray-200";

  return <div className={`${isDark ? darkClasses : lightClasses} ${className}`}>{children}</div>;
};
