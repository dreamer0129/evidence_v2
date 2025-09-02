# Design System Components for Evidence Pages

## Overview

This document outlines the reusable UI components for implementing the glassmorphism design across the evidence management pages.

## 1. Glass Container Component

```tsx
// components/GlassContainer.tsx
import { ReactNode } from "react";
import { motion } from "framer-motion";
import { cn } from "~~/lib/utils";

interface GlassContainerProps {
  children: ReactNode;
  className?: string;
  variant?: "card" | "modal" | "sheet";
  hover?: boolean;
  onClick?: () => void;
}

export const GlassContainer = ({
  children,
  className = "",
  variant = "card",
  hover = false,
  onClick,
}: GlassContainerProps) => {
  const baseClasses =
    "relative bg-black/40 backdrop-blur-md border border-white/10";

  const variantClasses = {
    card: "rounded-2xl p-6",
    modal: "rounded-3xl p-8",
    sheet: "rounded-xl p-4",
  };

  const hoverClasses = hover
    ? "hover:border-white/20 hover:shadow-2xl transition-all duration-300 hover:scale-[1.02] cursor-pointer"
    : "";

  return (
    <motion.div
      whileHover={hover ? { scale: 1.02 } : {}}
      whileTap={hover ? { scale: 0.98 } : {}}
      className={cn(
        baseClasses,
        variantClasses[variant],
        hoverClasses,
        className
      )}
      onClick={onClick}
    >
      {/* Grid Pattern Overlay */}
      <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGcgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIiBzdHJva2U9IiNmZmYiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMSI+PHBhdGggZD0iTTAgMGg0MHY0MEgweiIvPjwvZz48L3N2Zz4=')] opacity-10 rounded-inherit" />

      {/* Content */}
      <div className="relative z-10">{children}</div>
    </motion.div>
  );
};
```

## 2. Gradient Button Component

```tsx
// components/GradientButton.tsx
import { ReactNode } from "react";
import { motion } from "framer-motion";
import { Loader2 } from "lucide-react";
import { cn } from "~~/lib/utils";

interface GradientButtonProps {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  loading?: boolean;
  className?: string;
  size?: "sm" | "md" | "lg";
  variant?: "primary" | "secondary" | "success" | "danger";
}

export const GradientButton = ({
  children,
  onClick,
  disabled = false,
  loading = false,
  className = "",
  size = "md",
  variant = "primary",
}: GradientButtonProps) => {
  const gradients = {
    primary:
      "from-blue-500 to-purple-500 hover:from-blue-600 hover:to-purple-600",
    secondary:
      "from-gray-600 to-gray-700 hover:from-gray-700 hover:to-gray-800",
    success:
      "from-green-500 to-emerald-500 hover:from-green-600 hover:to-emerald-600",
    danger: "from-red-500 to-rose-500 hover:from-red-600 hover:to-rose-600",
  };

  const sizes = {
    sm: "px-4 py-2 text-sm",
    md: "px-6 py-3",
    lg: "px-8 py-4 text-lg",
  };

  return (
    <motion.button
      whileHover={{ scale: disabled || loading ? 1 : 1.05 }}
      whileTap={{ scale: disabled || loading ? 1 : 0.95 }}
      onClick={onClick}
      disabled={disabled || loading}
      className={cn(
        "relative inline-flex items-center justify-center rounded-xl font-medium text-white transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed",
        "bg-gradient-to-r",
        gradients[variant],
        sizes[size],
        className
      )}
    >
      {loading && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
      {children}

      {/* Shimmer Effect */}
      <motion.div
        className="absolute inset-0 rounded-xl bg-gradient-to-r from-white/0 via-white/20 to-white/0"
        initial={{ x: "-100%" }}
        animate={{ x: "100%" }}
        transition={{
          duration: 1.5,
          repeat: Infinity,
          ease: "linear",
          repeatDelay: 0.5,
        }}
        style={{ display: disabled || loading ? "none" : "block" }}
      />
    </motion.button>
  );
};
```

## 3. Status Badge Component

```tsx
// components/StatusBadge.tsx
import { ReactNode } from "react";
import { motion } from "framer-motion";
import { cn } from "~~/lib/utils";

interface StatusBadgeProps {
  status: "effective" | "revoked" | "expired" | "pending";
  children?: ReactNode;
  className?: string;
}

export const StatusBadge = ({
  status,
  children,
  className = "",
}: StatusBadgeProps) => {
  const statusConfig = {
    effective: {
      bg: "from-green-500/20 to-emerald-500/20",
      border: "border-green-500/30",
      text: "text-green-400",
      dot: "bg-green-400",
      label: "有效",
    },
    revoked: {
      bg: "from-red-500/20 to-rose-500/20",
      border: "border-red-500/30",
      text: "text-red-400",
      dot: "bg-red-400",
      label: "已撤销",
    },
    expired: {
      bg: "from-yellow-500/20 to-amber-500/20",
      border: "border-yellow-500/30",
      text: "text-yellow-400",
      dot: "bg-yellow-400",
      label: "已过期",
    },
    pending: {
      bg: "from-blue-500/20 to-cyan-500/20",
      border: "border-blue-500/30",
      text: "text-blue-400",
      dot: "bg-blue-400",
      label: "处理中",
    },
  };

  const config = statusConfig[status];

  return (
    <motion.span
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      className={cn(
        "inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-gradient-to-r border",
        config.bg,
        config.border,
        config.text,
        className
      )}
    >
      <motion.div
        className={cn("w-2 h-2 rounded-full mr-2", config.dot)}
        animate={{ scale: [1, 1.2, 1] }}
        transition={{ duration: 2, repeat: Infinity }}
      />
      {children || config.label}
    </motion.span>
  );
};
```

## 4. File Upload Area Component

```tsx
// components/FileUploadArea.tsx
import { useCallback, useState } from "react";
import { useDropzone } from "react-dropzone";
import { motion, AnimatePresence } from "framer-motion";
import { Upload, File, X } from "lucide-react";
import { cn } from "~~/lib/utils";

interface FileUploadAreaProps {
  onFileSelect: (file: File) => void;
  acceptedTypes?: string[];
  maxSize?: number;
  className?: string;
}

export const FileUploadArea = ({
  onFileSelect,
  acceptedTypes = ["*"],
  maxSize = 100 * 1024 * 1024, // 100MB
  className = "",
}: FileUploadAreaProps) => {
  const [file, setFile] = useState<File | null>(null);
  const [isDragActive, setIsDragActive] = useState(false);

  const onDrop = useCallback(
    (acceptedFiles: File[]) => {
      if (acceptedFiles.length > 0) {
        const selectedFile = acceptedFiles[0];
        setFile(selectedFile);
        onFileSelect(selectedFile);
      }
      setIsDragActive(false);
    },
    [onFileSelect]
  );

  const { getRootProps, getInputProps } = useDropzone({
    onDrop,
    onDragEnter: () => setIsDragActive(true),
    onDragLeave: () => setIsDragActive(false),
    accept: acceptedTypes.reduce((acc, type) => ({ ...acc, [type]: [] }), {}),
    maxSize,
    multiple: false,
  });

  const removeFile = () => {
    setFile(null);
  };

  return (
    <div className={cn("w-full", className)}>
      <div
        {...getRootProps()}
        className={cn(
          "relative group cursor-pointer transition-all duration-300",
          isDragActive && "scale-105"
        )}
      >
        {/* Background Glow */}
        <div className="absolute inset-0 bg-gradient-to-r from-blue-500/20 to-purple-500/20 rounded-2xl blur-xl group-hover:from-blue-500/30 group-hover:to-purple-500/30 transition-all duration-500" />

        {/* Upload Area */}
        <div
          className={cn(
            "relative bg-black/40 backdrop-blur-md border-2 border-dashed rounded-2xl p-12 transition-all duration-300",
            isDragActive
              ? "border-blue-400/60 bg-blue-500/10"
              : "border-white/20 hover:border-white/40"
          )}
        >
          <input {...getInputProps()} />

          <AnimatePresence mode="wait">
            {!file ? (
              <motion.div
                key="upload"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center"
              >
                <motion.div
                  animate={{ y: isDragActive ? -10 : 0 }}
                  className="w-20 h-20 mx-auto mb-6 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center group-hover:scale-110 transition-transform duration-300"
                >
                  <Upload className="w-10 h-10 text-white" />
                </motion.div>

                <p className="text-xl text-white text-center mb-2">
                  {isDragActive ? "释放文件上传" : "拖拽文件到此处"}
                </p>
                <p className="text-gray-400 text-center mb-6">或点击选择文件</p>

                <div className="flex justify-center gap-2 flex-wrap">
                  {acceptedTypes.map((type) => (
                    <span
                      key={type}
                      className="px-3 py-1 bg-white/10 rounded-full text-xs text-gray-300"
                    >
                      {type === "*"
                        ? "所有格式"
                        : type.split("/")[1]?.toUpperCase() || type}
                    </span>
                  ))}
                </div>
              </motion.div>
            ) : (
              <motion.div
                key="file"
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex items-center justify-between"
              >
                <div className="flex items-center space-x-4">
                  <div className="w-16 h-16 bg-gradient-to-br from-blue-500/20 to-purple-500/20 rounded-xl flex items-center justify-center">
                    <File className="w-8 h-8 text-blue-400" />
                  </div>
                  <div className="text-left">
                    <p className="text-white font-medium">{file.name}</p>
                    <p className="text-sm text-gray-400">
                      {(file.size / 1024 / 1024).toFixed(2)} MB
                    </p>
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    removeFile();
                  }}
                  className="p-2 rounded-lg bg-red-500/20 hover:bg-red-500/30 transition-colors"
                >
                  <X className="w-5 h-5 text-red-400" />
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
};
```

## 5. Loading Skeleton Component

```tsx
// components/LoadingSkeleton.tsx
import { motion } from "framer-motion";
import { cn } from "~~/lib/utils";

interface LoadingSkeletonProps {
  className?: string;
  lines?: number;
  height?: string;
  animate?: boolean;
}

export const LoadingSkeleton = ({
  className = "",
  lines = 1,
  height = "1rem",
  animate = true,
}: LoadingSkeletonProps) => {
  const skeletonLines = Array.from({ length: lines }, (_, i) => i);

  return (
    <div className={cn("space-y-3", className)}>
      {skeletonLines.map((index) => (
        <motion.div
          key={index}
          className={cn(
            "bg-gradient-to-r from-gray-700 via-gray-600 to-gray-700 rounded",
            animate && "animate-pulse"
          )}
          style={{ height }}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: index * 0.1 }}
        />
      ))}
    </div>
  );
};
```

## 6. Animated Counter Component

```tsx
// components/AnimatedCounter.tsx
import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { cn } from "~~/lib/utils";

interface AnimatedCounterProps {
  target: number;
  duration?: number;
  className?: string;
  prefix?: string;
  suffix?: string;
  decimals?: number;
}

export const AnimatedCounter = ({
  target,
  duration = 2000,
  className = "",
  prefix = "",
  suffix = "",
  decimals = 0,
}: AnimatedCounterProps) => {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let startTime: number;
    const animateCount = (timestamp: number) => {
      if (!startTime) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / duration, 1);

      // Easing function for smooth animation
      const easeOutQuart = 1 - Math.pow(1 - progress, 4);
      setCount(target * easeOutQuart);

      if (progress < 1) {
        requestAnimationFrame(animateCount);
      }
    };

    requestAnimationFrame(animateCount);
  }, [target, duration]);

  return (
    <motion.span
      className={cn("font-bold", className)}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      {prefix}
      {count.toFixed(decimals)}
      {suffix}
    </motion.span>
  );
};
```

## 7. Toast Notification Component

```tsx
// components/ToastNotification.tsx
import { useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CheckCircle, XCircle, AlertCircle, Info, X } from "lucide-react";
import { cn } from "~~/lib/utils";

interface ToastProps {
  id: string;
  type: "success" | "error" | "warning" | "info";
  title: string;
  message?: string;
  duration?: number;
  onClose: (id: string) => void;
}

export const ToastNotification = ({
  id,
  type,
  title,
  message,
  duration = 5000,
  onClose,
}: ToastProps) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose(id);
    }, duration);

    return () => clearTimeout(timer);
  }, [id, duration, onClose]);

  const typeConfig = {
    success: {
      icon: CheckCircle,
      bg: "from-green-500/20 to-emerald-500/20",
      border: "border-green-500/30",
      iconColor: "text-green-400",
    },
    error: {
      icon: XCircle,
      bg: "from-red-500/20 to-rose-500/20",
      border: "border-red-500/30",
      iconColor: "text-red-400",
    },
    warning: {
      icon: AlertCircle,
      bg: "from-yellow-500/20 to-amber-500/20",
      border: "border-yellow-500/30",
      iconColor: "text-yellow-400",
    },
    info: {
      icon: Info,
      bg: "from-blue-500/20 to-cyan-500/20",
      border: "border-blue-500/30",
      iconColor: "text-blue-400",
    },
  };

  const config = typeConfig[type];
  const Icon = config.icon;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, x: 100 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: 100 }}
      className="relative overflow-hidden"
    >
      <div
        className={cn(
          "relative bg-black/40 backdrop-blur-md border rounded-xl p-4 min-w-[300px] max-w-md",
          config.bg,
          config.border
        )}
      >
        <div className="flex items-start">
          <Icon
            className={cn("w-5 h-5 mt-0.5 flex-shrink-0", config.iconColor)}
          />

          <div className="ml-3 flex-1">
            <h4 className="text-sm font-medium text-white">{title}</h4>
            {message && <p className="mt-1 text-sm text-gray-300">{message}</p>}
          </div>

          <button
            onClick={() => onClose(id)}
            className="ml-4 flex-shrink-0 text-gray-400 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Progress Bar */}
        <motion.div
          className="absolute bottom-0 left-0 h-0.5 bg-gradient-to-r from-blue-500 to-purple-500"
          initial={{ width: "100%" }}
          animate={{ width: "0%" }}
          transition={{ duration: duration / 1000, ease: "linear" }}
        />
      </div>
    </motion.div>
  );
};
```

## 8. Page Background Component

```tsx
// components/PageBackground.tsx
import { ReactNode } from "react";
import { WavyBackground } from "./ui/wavy-background";

interface PageBackgroundProps {
  children: ReactNode;
  className?: string;
}

export const PageBackground = ({
  children,
  className = "",
}: PageBackgroundProps) => {
  return (
    <div className={cn("min-h-screen relative", className)}>
      {/* Dynamic Background */}
      <div className="fixed inset-0 z-0">
        <WavyBackground
          colors={["#3b82f6", "#8b5cf6", "#06b6d4", "#10b981", "#f59e0b"]}
          waveWidth={50}
          backgroundFill="black"
          blur={10}
          speed="medium"
          waveOpacity={0.2}
        >
          <div />
        </WavyBackground>
      </div>

      {/* Content */}
      <div className="relative z-10">{children}</div>
    </div>
  );
};
```

## Usage Examples

### Example 1: Upload Page Structure

```tsx
import { PageBackground } from "~/components/PageBackground";
import { GlassContainer } from "~/components/GlassContainer";
import { GradientButton } from "~/components/GradientButton";
import { FileUploadArea } from "~/components/FileUploadArea";
import { StatusBadge } from "~/components/StatusBadge";

const UploadPage = () => {
  return (
    <PageBackground>
      <div className="pt-20 pb-12">
        <div className="max-w-4xl mx-auto px-4">
          {/* Hero Section */}
          <div className="text-center mb-12">
            <h1 className="text-5xl font-bold text-white mb-4">
              创建您的数字存证
            </h1>
            <p className="text-xl text-gray-300">
              安全、快速地将您的重要文件或数据哈希上链
            </p>
          </div>

          {/* Main Content */}
          <GlassContainer variant="card" className="mb-6">
            <FileUploadArea
              onFileSelect={(file) => console.log(file)}
              acceptedTypes={["image/*", "application/pdf"]}
            />
          </GlassContainer>

          {/* Actions */}
          <div className="flex justify-end">
            <GradientButton size="lg">提交存证</GradientButton>
          </div>
        </div>
      </div>
    </PageBackground>
  );
};
```

### Example 2: Status Display

```tsx
const EvidenceStatus = () => {
  return (
    <GlassContainer variant="card">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-white mb-1">存证状态</h3>
          <p className="text-sm text-gray-400">最后更新: 2024-01-15 14:30</p>
        </div>

        <StatusBadge status="effective" />
      </div>
    </GlassContainer>
  );
};
```

## Customization Guidelines

1. **Colors**: Modify gradient colors in each component to match your brand
2. **Spacing**: Adjust padding and margins through className props
3. **Animations**: Control animation duration and easing through props
4. **Variants**: Add new variants (e.g., new status types) as needed
5. **Accessibility**: Ensure all components meet WCAG 2.1 standards

## Performance Considerations

1. Use `motion` components only when animations are needed
2. Implement lazy loading for heavy components
3. Use `React.memo` for components that don't change often
4. Optimize animation performance with `will-change` CSS property when necessary
