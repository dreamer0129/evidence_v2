"use client";

import React from "react";
import { useTheme } from "next-themes";

interface AdaptiveTextProps {
  children: React.ReactNode;
  className?: string;
  variant?: "primary" | "secondary" | "muted";
}

export const AdaptiveText = ({ children, className = "", variant = "primary" }: AdaptiveTextProps) => {
  const { theme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  // 避免服务器端渲染不匹配
  React.useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    // 默认返回浅色主题样式
    return <span className={className}>{children}</span>;
  }

  const isDark = theme === "dark";

  // 深色主题样式（适配主页）
  const darkClasses = {
    primary: "text-white",
    secondary: "text-gray-300",
    muted: "text-gray-400",
  };

  // 浅色主题样式（适配其他页面）
  const lightClasses = {
    primary: "text-gray-800",
    secondary: "text-gray-600",
    muted: "text-gray-500",
  };

  const textClass = isDark ? darkClasses[variant] : lightClasses[variant];

  return <span className={`${textClass} ${className}`}>{children}</span>;
};
