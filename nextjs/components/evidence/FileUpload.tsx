"use client";

import React, { useRef, useState } from "react";
import { GlassContainer } from "./GlassContainer";
import { AnimatePresence, motion } from "framer-motion";
import { AlertCircle, CheckCircle, File, Upload, X } from "lucide-react";
import { useDropzone } from "react-dropzone";
import { cn } from "~~/lib/utils";

interface FileUploadProps {
  onChange?: (files: File[]) => void;
  maxSize?: number; // in MB
  acceptedTypes?: string[];
  className?: string;
}

export const FileUpload: React.FC<FileUploadProps> = ({
  onChange,
  maxSize = 100,
  acceptedTypes = ["*"],
  className = "",
}) => {
  const [files, setFiles] = useState<File[]>([]);
  const [isDragActive, setIsDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (newFiles: File[]) => {
    setError(null);

    // Validate file size
    const oversizedFiles = newFiles.filter(file => file.size > maxSize * 1024 * 1024);
    if (oversizedFiles.length > 0) {
      setError(`文件大小不能超过 ${maxSize}MB`);
      return;
    }

    // Validate file types if specified
    if (acceptedTypes[0] !== "*") {
      const invalidFiles = newFiles.filter(file => {
        const extension = "." + file.name.split(".").pop()?.toLowerCase();
        return !acceptedTypes.includes(extension) && !acceptedTypes.includes(file.type);
      });
      if (invalidFiles.length > 0) {
        setError(`不支持的文件类型`);
        return;
      }
    }

    // Keep only the first file
    const fileToKeep = newFiles.length > 0 ? [newFiles[0]] : [];
    setFiles(fileToKeep);
    onChange?.(fileToKeep);
  };

  const removeFile = () => {
    setFiles([]);
    onChange?.([]);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const { getRootProps, getInputProps } = useDropzone({
    multiple: false,
    noClick: false,
    onDrop: handleFileChange,
    onDropRejected: fileRejections => {
      const rejection = fileRejections[0];
      if (rejection.errors[0].code === "file-too-large") {
        setError(`文件大小不能超过 ${maxSize}MB`);
      } else if (rejection.errors[0].code === "file-invalid-type") {
        setError("不支持的文件类型");
      }
    },
    onDragEnter: () => setIsDragActive(true),
    onDragLeave: () => setIsDragActive(false),
  });

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  return (
    <div className="w-full" {...getRootProps()}>
      <GlassContainer
        className={cn(
          "relative cursor-pointer transition-all duration-300",
          files.length === 0 ? "min-h-[200px]" : "min-h-[100px]",
          isDragActive && "ring-2 ring-blue-400 ring-offset-2",
          error && "ring-2 ring-red-400 ring-offset-2",
          className,
        )}
        hover
        intensity="low"
      >
        <input ref={fileInputRef} {...getInputProps()} className="hidden" />

        {/* Background pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="w-full h-full bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSIjOUM5MkFDIiBmaWxsLW9wYWNpdHk9IjAuMSI+PHBhdGggZD0iTTAgNDBMNDAgMEgyMEwwIDIwTTQwIDRWMjBMMjAgNDAiLz48L2c+PC9zdmc+')]" />
        </div>

        {/* Drag overlay */}
        <AnimatePresence>
          {isDragActive && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 bg-blue-500/20 rounded-2xl flex items-center justify-center"
            >
              <motion.div initial={{ scale: 0.8 }} animate={{ scale: 1 }} className="text-blue-600 dark:text-blue-400">
                <Upload className="w-12 h-12" />
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>

        <div className={cn("relative z-10", files.length === 0 ? "p-8" : "p-4")}>
          <AnimatePresence mode="wait">
            {files.length === 0 ? (
              <motion.div
                key="upload-area"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="text-center"
              >
                <motion.div
                  className="mx-auto w-16 h-16 mb-4 text-blue-500 dark:text-blue-400"
                  whileHover={{ scale: 1.1 }}
                  whileTap={{ scale: 0.9 }}
                >
                  <Upload className="w-full h-full" />
                </motion.div>

                <motion.h3
                  className="text-lg font-semibold text-gray-700 dark:text-gray-200 mb-2"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.1 }}
                >
                  拖拽文件到此处
                </motion.h3>

                <motion.p
                  className="text-sm text-gray-500 dark:text-gray-400 mb-4"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.2 }}
                >
                  或点击选择文件
                </motion.p>

                <motion.p
                  className="text-xs text-gray-400 dark:text-gray-500"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.3 }}
                >
                  支持 PDF, DOC, JPG, PNG 等格式，最大 {maxSize}MB
                </motion.p>
              </motion.div>
            ) : (
              <motion.div
                key="file-preview"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                className="space-y-4"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3 flex-1">
                    <motion.div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg" whileHover={{ scale: 1.05 }}>
                      <File className="w-6 h-6 text-blue-600 dark:text-blue-400" />
                    </motion.div>

                    <div className="flex-1 min-w-0">
                      <motion.h4
                        className="text-sm font-medium text-gray-700 dark:text-gray-200 truncate"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                      >
                        {files[0].name}
                      </motion.h4>

                      <motion.div
                        className="flex items-center space-x-4 mt-1"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.1 }}
                      >
                        <span className="text-xs text-gray-500 dark:text-gray-400">
                          {formatFileSize(files[0].size)}
                        </span>
                        <span className="text-xs text-gray-400">•</span>
                        <span className="text-xs text-gray-500 dark:text-gray-400">{files[0].type || "未知类型"}</span>
                      </motion.div>
                    </div>
                  </div>

                  <motion.button
                    onClick={e => {
                      e.stopPropagation();
                      removeFile();
                    }}
                    className="p-1 text-gray-400 hover:text-red-500 transition-colors"
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                  >
                    <X className="w-5 h-5" />
                  </motion.button>
                </div>

                <motion.div
                  className="flex items-center space-x-2 text-xs text-green-600 dark:text-green-400"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.2 }}
                >
                  <CheckCircle className="w-4 h-4" />
                  <span>文件已准备就绪</span>
                </motion.div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Error message */}
          <AnimatePresence>
            {error && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="mt-4 flex items-center space-x-2 text-red-600 dark:text-red-400"
              >
                <AlertCircle className="w-4 h-4" />
                <span className="text-sm">{error}</span>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </GlassContainer>
    </div>
  );
};
