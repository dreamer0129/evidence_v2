"use client";

import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { AlertCircle, CheckCircle, Clock, FileText, RefreshCw, Shield, X } from "lucide-react";

interface CertificateModalProps {
  isOpen: boolean;
  onClose: () => void;
  evidenceId: string;
  fileName?: string;
}

interface CertificateMetadata {
  evidenceId: string;
  fileName: string;
  fileSize: string;
  createdAt: string;
  status: "verified" | "pending" | "error";
  blockchainTx?: string;
}

// Loading skeleton component
const CertificateLoadingSkeleton = () => (
  <div className="space-y-4">
    <div className="animate-pulse">
      <div className="h-4 bg-gray-300 dark:bg-gray-700 rounded w-3/4 mb-2"></div>
      <div className="h-3 bg-gray-300 dark:bg-gray-700 rounded w-1/2"></div>
    </div>
    <div className="animate-pulse">
      <div className="h-32 bg-gray-300 dark:bg-gray-700 rounded-lg"></div>
    </div>
    <div className="animate-pulse grid grid-cols-2 gap-4">
      <div className="h-3 bg-gray-300 dark:bg-gray-700 rounded"></div>
      <div className="h-3 bg-gray-300 dark:bg-gray-700 rounded"></div>
    </div>
  </div>
);

// Status badge component
const StatusBadge = ({ status }: { status: "verified" | "pending" | "error" }) => {
  const variants = {
    verified: {
      bg: "bg-green-100 dark:bg-green-900/30",
      text: "text-green-800 dark:text-green-400",
      icon: CheckCircle,
      label: "已验证",
    },
    pending: {
      bg: "bg-yellow-100 dark:bg-yellow-900/30",
      text: "text-yellow-800 dark:text-yellow-400",
      icon: Clock,
      label: "验证中",
    },
    error: {
      bg: "bg-red-100 dark:bg-red-900/30",
      text: "text-red-800 dark:text-red-400",
      icon: AlertCircle,
      label: "验证失败",
    },
  };

  const variant = variants[status];
  const Icon = variant.icon;

  return (
    <motion.div
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      className={`inline-flex items-center px-3 py-1.5 rounded-full text-sm font-medium ${variant.bg} ${variant.text}`}
    >
      <Icon className="w-4 h-4 mr-1.5" />
      {variant.label}
    </motion.div>
  );
};

export const CertificateModal = ({ isOpen, onClose, evidenceId, fileName }: CertificateModalProps) => {
  const [certificateUrl, setCertificateUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [metadata, setMetadata] = useState<CertificateMetadata | null>(null);

  // Mock metadata - in real app this would come from API
  const mockMetadata: CertificateMetadata = {
    evidenceId,
    fileName: fileName || "未命名文件",
    fileSize: "2.4 MB",
    createdAt: new Date().toLocaleDateString("zh-CN"),
    status: "verified",
    blockchainTx: "0x1234...5678",
  };

  // Fetch certificate when modal opens
  useEffect(() => {
    if (isOpen) {
      setIsLoading(true);
      setError(null);
      setMetadata(mockMetadata);

      const fetchCertificate = async () => {
        try {
          const token = localStorage.getItem("token");
          if (!token) {
            setError("请先登录");
            setIsLoading(false);
            return;
          }

          const response = await fetch(
            `http://localhost:8080/api/evidence/evidenceId/${evidenceId}/certificate/download`,
            {
              method: "GET",
              headers: {
                Authorization: `Bearer ${token}`,
              },
            },
          );

          if (response.ok) {
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            setCertificateUrl(url);
          } else if (response.status === 401) {
            setError("登录已过期，请重新登录");
            localStorage.removeItem("token");
          } else if (response.status === 404) {
            setError("证书不存在");
          } else {
            setError("获取证书失败");
          }
        } catch (error) {
          console.error("Error fetching certificate:", error);
          setError("网络错误，请检查后端服务");
        } finally {
          setIsLoading(false);
        }
      };

      fetchCertificate();
    }
  }, [isOpen, evidenceId, fileName]);

  // Clean up URL when modal closes
  const handleClose = () => {
    if (certificateUrl) {
      URL.revokeObjectURL(certificateUrl);
      setCertificateUrl(null);
    }
    setError(null);
    setIsLoading(false);
    setMetadata(null);
    onClose();
  };

  // Retry loading
  const handleRetry = () => {
    if (isOpen) {
      setIsLoading(true);
      setError(null);

      const fetchCertificate = async () => {
        try {
          const token = localStorage.getItem("token");
          if (!token) {
            setError("请先登录");
            setIsLoading(false);
            return;
          }

          const response = await fetch(
            `http://localhost:8080/api/evidence/evidenceId/${evidenceId}/certificate/download`,
            {
              method: "GET",
              headers: {
                Authorization: `Bearer ${token}`,
              },
            },
          );

          if (response.ok) {
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            setCertificateUrl(url);
          } else if (response.status === 401) {
            setError("登录已过期，请重新登录");
            localStorage.removeItem("token");
          } else if (response.status === 404) {
            setError("证书不存在");
          } else {
            setError("获取证书失败");
          }
        } catch (error) {
          console.error("Error fetching certificate:", error);
          setError("网络错误，请检查后端服务");
        } finally {
          setIsLoading(false);
        }
      };

      fetchCertificate();
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 flex items-start justify-center pt-16 px-4 bg-black/60 backdrop-blur-sm"
        onClick={handleClose}
      >
        <motion.div
          initial={{ scale: 0.9, opacity: 0, y: 20 }}
          animate={{ scale: 1, opacity: 1, y: 0 }}
          exit={{ scale: 0.9, opacity: 0, y: 20 }}
          transition={{ type: "spring", damping: 25, stiffness: 300 }}
          className="relative w-full max-w-6xl max-h-[60vh]"
          onClick={e => e.stopPropagation()}
        >
          <div className="bg-white/95 dark:bg-gray-800/95 backdrop-blur-md rounded-2xl border border-gray-200/50 dark:border-gray-700/50 shadow-2xl overflow-hidden">
            {/* Header */}
            <div className="bg-gradient-to-r from-blue-600 to-purple-600 p-4 text-white">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{ delay: 0.2, type: "spring", stiffness: 400, damping: 25 }}
                    className="p-2 bg-white/20 rounded-lg backdrop-blur-sm"
                  >
                    <Shield className="w-6 h-6 text-white" />
                  </motion.div>
                  <div>
                    <div className="flex items-center gap-3">
                      <motion.h2
                        initial={{ y: -10, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        transition={{ delay: 0.1 }}
                        className="text-sm font-semibold"
                      >
                        区块链证书验证
                      </motion.h2>
                      {metadata && (
                        <motion.div
                          initial={{ y: -10, opacity: 0 }}
                          animate={{ y: 0, opacity: 1 }}
                          transition={{ delay: 0.3 }}
                          className="flex items-center"
                        >
                          <StatusBadge status={metadata.status} />
                        </motion.div>
                      )}
                    </div>
                    <motion.p
                      initial={{ y: -10, opacity: 0 }}
                      animate={{ y: 0, opacity: 1 }}
                      transition={{ delay: 0.2 }}
                      className="text-blue-100 text-xs mt-1"
                    >
                      由区块链技术确保的不可篡改数字证书
                    </motion.p>
                  </div>
                </div>

                <motion.button
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ delay: 0.4 }}
                  whileHover={{ scale: 1.1 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={handleClose}
                  className="p-1.5 bg-white/20 hover:bg-white/30 rounded-lg backdrop-blur-sm transition-colors"
                >
                  <X className="w-5 h-5 text-white" />
                </motion.button>
              </div>
            </div>

            {/* Content */}
            <div className="p-6">
              {/* Certificate Viewer */}
              <div className="bg-white dark:bg-gray-900 rounded-xl shadow-inner border border-gray-200 dark:border-gray-700 overflow-hidden">
                <div className="h-[80vh] relative">
                  {isLoading ? (
                    <div className="flex items-center justify-center h-full">
                      <div className="text-center max-w-md">
                        <motion.div
                          animate={{ rotate: 360 }}
                          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                          className="w-16 h-16 mx-auto mb-4"
                        >
                          <RefreshCw className="w-full h-full text-blue-500" />
                        </motion.div>
                        <CertificateLoadingSkeleton />
                        <p className="text-gray-600 dark:text-gray-400 mt-4">正在加载证书...</p>
                      </div>
                    </div>
                  ) : error ? (
                    <div className="flex items-center justify-center h-full">
                      <div className="text-center max-w-md">
                        <motion.div
                          initial={{ scale: 0 }}
                          animate={{ scale: 1 }}
                          transition={{ type: "spring", stiffness: 400, damping: 25 }}
                        >
                          <AlertCircle className="w-16 h-16 mx-auto mb-4 text-red-500" />
                        </motion.div>
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">加载失败</h3>
                        <p className="text-gray-600 dark:text-gray-400 mb-6">{error}</p>
                        <motion.button
                          whileHover={{ scale: 1.05 }}
                          whileTap={{ scale: 0.95 }}
                          onClick={handleRetry}
                          className="inline-flex items-center px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
                        >
                          <RefreshCw className="w-4 h-4 mr-2" />
                          重试加载
                        </motion.button>
                      </div>
                    </div>
                  ) : certificateUrl ? (
                    <motion.div
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ duration: 0.3 }}
                      className="h-full"
                    >
                      <iframe
                        src={certificateUrl}
                        className="w-full h-full border-0"
                        title={`证书 - ${fileName || evidenceId}`}
                      />
                    </motion.div>
                  ) : (
                    <div className="flex items-center justify-center h-full">
                      <div className="text-center">
                        <FileText className="w-16 h-16 mx-auto mb-4 text-gray-400" />
                        <p className="text-gray-600 dark:text-gray-400">无法加载证书</p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};
