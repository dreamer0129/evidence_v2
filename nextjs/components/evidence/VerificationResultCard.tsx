"use client";

import React, { useState } from "react";
import { GlassCard } from "./GlassContainer";
import { AnimatePresence, motion } from "framer-motion";
import { Blocks, Calendar, CheckCircle, Copy, Database, FileText, Hash, XCircle } from "lucide-react";
import { cn } from "~~/lib/utils";

interface VerificationResult {
  success: boolean;
  fileName: string;
  timestamp: string;
  fileHash: string;
  evidenceId: string;
  blockHeight: number;
  status: string;
  error?: string;
}

interface VerificationResultCardProps {
  result: VerificationResult | null;
  isVerifying?: boolean;
  className?: string;
}

export const VerificationResultCard: React.FC<VerificationResultCardProps> = ({
  result,
  isVerifying = false,
  className = "",
}) => {
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const handleCopy = async (text: string, field: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedField(field);
      setTimeout(() => setCopiedField(null), 2000);
    } catch (error) {
      console.error("Failed to copy:", error);
    }
  };

  if (isVerifying) {
    return (
      <GlassCard className={cn("text-center", className)}>
        <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="py-12">
          <motion.div
            className="w-20 h-20 mx-auto mb-6 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 flex items-center justify-center"
            animate={{ rotate: 360 }}
            transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
          >
            <Hash className="w-10 h-10 text-white" />
          </motion.div>

          <motion.h3
            className="text-xl font-semibold text-black dark:text-white mb-2"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
          >
            正在验证...
          </motion.h3>

          <motion.p
            className="text-gray-800 dark:text-gray-300"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
          >
            请稍候，正在查询区块链上的存证记录
          </motion.p>

          {/* Animated progress dots */}
          <motion.div className="flex justify-center space-x-2 mt-6">
            {[...Array(3)].map((_, i) => (
              <motion.div
                key={i}
                className="w-2 h-2 bg-blue-400 rounded-full"
                animate={{
                  scale: [1, 1.5, 1],
                  opacity: [0.5, 1, 0.5],
                }}
                transition={{
                  duration: 1.5,
                  repeat: Infinity,
                  delay: i * 0.2,
                }}
              />
            ))}
          </motion.div>
        </motion.div>
      </GlassCard>
    );
  }

  if (!result) return null;

  return (
    <AnimatePresence mode="wait">
      <motion.div
        key={result.success ? "success" : "error"}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        className={cn("relative overflow-hidden", className)}
      >
        {/* Background gradient based on result */}
        <div
          className={cn(
            "absolute inset-0 rounded-2xl",
            result.success
              ? "bg-gradient-to-br from-green-500/20 to-emerald-500/20"
              : "bg-gradient-to-br from-red-500/20 to-rose-500/20",
          )}
        />

        {/* Animated particles for success */}
        {result.success && (
          <div className="absolute inset-0 overflow-hidden rounded-2xl">
            {[...Array(20)].map((_, i) => (
              <motion.div
                key={i}
                className="absolute w-1 h-1 bg-green-400 rounded-full"
                style={{
                  left: `${Math.random() * 100}%`,
                  top: `${Math.random() * 100}%`,
                }}
                animate={{
                  y: [0, -100],
                  opacity: [0, 1, 0],
                  scale: [0, 1, 0.5],
                }}
                transition={{
                  duration: 2,
                  delay: Math.random() * 2,
                  repeat: Infinity,
                  repeatDelay: Math.random() * 3,
                }}
              />
            ))}
          </div>
        )}

        <GlassCard intensity="high" className="relative">
          {/* Result icon and title */}
          <div className="text-center mb-8">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{
                type: "spring",
                stiffness: 300,
                damping: 20,
                delay: 0.2,
              }}
              className={cn(
                "w-24 h-24 mx-auto mb-6 rounded-full flex items-center justify-center",
                result.success
                  ? "bg-gradient-to-br from-green-500 to-emerald-500"
                  : "bg-gradient-to-br from-red-500 to-rose-500",
              )}
            >
              {result.success ? (
                <CheckCircle className="w-12 h-12 text-white" />
              ) : (
                <XCircle className="w-12 h-12 text-white" />
              )}
            </motion.div>

            <motion.h3
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className={cn("text-2xl font-bold mb-2", result.success ? "text-green-400" : "text-red-400")}
            >
              {result.success ? "验证成功" : "验证失败"}
            </motion.h3>

            <motion.p
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="text-gray-800 dark:text-gray-300"
            >
              {result.success ? "文件完整性验证通过，存证有效" : result.error || "未找到匹配的存证记录"}
            </motion.p>
          </div>

          {/* Details */}
          {result.success && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
              className="space-y-4"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* File Name */}
                <div className="bg-gray-50 dark:bg-black/20 rounded-lg p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center space-x-2 mb-2">
                    <FileText className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                    <span className="text-sm text-gray-600 dark:text-gray-400">文件名称</span>
                  </div>
                  <p className="text-gray-900 dark:text-white font-medium truncate">{result.fileName}</p>
                </div>

                {/* Timestamp */}
                <div className="bg-gray-50 dark:bg-black/20 rounded-lg p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center space-x-2 mb-2">
                    <Calendar className="w-4 h-4 text-purple-600 dark:text-purple-400" />
                    <span className="text-sm text-gray-600 dark:text-gray-400">存证时间</span>
                  </div>
                  <p className="text-gray-900 dark:text-white font-medium">{result.timestamp}</p>
                </div>

                {/* Evidence ID */}
                <div className="bg-gray-50 dark:bg-black/20 rounded-lg p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <Database className="w-4 h-4 text-green-600 dark:text-green-400" />
                      <span className="text-sm text-gray-600 dark:text-gray-400">存证ID</span>
                    </div>
                    <button
                      onClick={() => handleCopy(result.evidenceId, "evidenceId")}
                      className="text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
                    >
                      <Copy className="w-4 h-4" />
                    </button>
                  </div>
                  <div className="flex items-center space-x-2">
                    <code className="text-blue-600 dark:text-blue-400 font-mono text-sm flex-1 truncate">{result.evidenceId}</code>
                    {copiedField === "evidenceId" && <span className="text-xs text-green-600 dark:text-green-400">已复制</span>}
                  </div>
                </div>

                {/* Block Height */}
                <div className="bg-gray-50 dark:bg-black/20 rounded-lg p-4 border border-gray-200 dark:border-white/10">
                  <div className="flex items-center space-x-2 mb-2">
                    <Blocks className="w-4 h-4 text-orange-600 dark:text-orange-400" />
                    <span className="text-sm text-gray-600 dark:text-gray-400">区块高度</span>
                  </div>
                  <p className="text-gray-900 dark:text-white font-medium">#{result.blockHeight}</p>
                </div>
              </div>

              {/* File Hash */}
              <div className="bg-gray-50 dark:bg-black/20 rounded-lg p-4 border border-gray-200 dark:border-white/10">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-2">
                    <Hash className="w-4 h-4 text-cyan-600 dark:text-cyan-400" />
                    <span className="text-sm text-gray-600 dark:text-gray-400">文件哈希 (SHA256)</span>
                  </div>
                  <button
                    onClick={() => handleCopy(result.fileHash, "fileHash")}
                    className="text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
                  >
                    <Copy className="w-4 h-4" />
                  </button>
                </div>
                <div className="space-y-2">
                  <code className="text-blue-600 dark:text-blue-400 font-mono text-xs break-all block">{result.fileHash}</code>
                  {copiedField === "fileHash" && <span className="text-xs text-green-600 dark:text-green-400">哈希值已复制</span>}
                </div>
              </div>

              {/* Status */}
              <div className="flex items-center justify-center space-x-2 py-2">
                <div className="w-2 h-2 bg-green-600 dark:bg-green-400 rounded-full animate-pulse" />
                <span className="text-green-600 dark:text-green-400 font-medium">{result.status}</span>
              </div>
            </motion.div>
          )}

          {/* Error details */}
          {!result.success && result.fileHash && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
              className="mt-6 bg-gray-50 dark:bg-black/20 rounded-lg p-4 border border-gray-200 dark:border-white/10"
            >
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">查询的哈希值:</p>
              <code className="text-xs text-red-600 dark:text-red-400 font-mono break-all">{result.fileHash}</code>
            </motion.div>
          )}
        </GlassCard>
      </motion.div>
    </AnimatePresence>
  );
};
