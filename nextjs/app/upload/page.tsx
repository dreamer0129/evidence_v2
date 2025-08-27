"use client";

import { useState } from "react";
import type { NextPage } from "next";
import { DocumentArrowUpIcon, HashtagIcon } from "@heroicons/react/24/outline";

const Upload: NextPage = () => {
  const [activeTab, setActiveTab] = useState("file");

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6 lg:p-8">
      <div className="text-center">
        <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl">创建您的数字存证</h1>
        <p className="mt-3 text-lg text-gray-600">安全、快速地将您的重要文件或数据哈希上链</p>
      </div>

      <div className="mt-12">
        <div className="tabs tabs-boxed bg-base-200 p-2 rounded-lg justify-center">
          <a className={`tab tab-lg ${activeTab === "file" ? "tab-active" : ""}`} onClick={() => setActiveTab("file")}>
            <DocumentArrowUpIcon className="w-6 h-6 mr-2" />
            文件上传
          </a>
          <a className={`tab tab-lg ${activeTab === "hash" ? "tab-active" : ""}`} onClick={() => setActiveTab("hash")}>
            <HashtagIcon className="w-6 h-6 mr-2" />
            哈希上传
          </a>
        </div>
      </div>

      <div className="mt-6">
        <div className="card bg-base-100 shadow-xl w-full">
          <div className="card-body">
            {activeTab === "file" && (
              <div className="space-y-6">
                <div>
                  <h2 className="text-xl font-semibold">文件上传存证</h2>
                  <p className="text-gray-500 mt-1">选择您要存证的文件，系统将自动计算哈希值并上链。</p>
                </div>
                <div className="form-control">
                  <label htmlFor="file-upload" className="block text-sm font-medium text-gray-700 mb-2">
                    选择文件
                  </label>
                  <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md">
                    <div className="space-y-1 text-center">
                      <DocumentArrowUpIcon className="mx-auto h-12 w-12 text-gray-400" />
                      <div className="flex items-center text-sm text-gray-600">
                        <label
                          htmlFor="file-upload"
                          className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-indigo-500"
                        >
                          <span>上传文件</span>
                          <input id="file-upload" name="file-upload" type="file" className="sr-only" />
                        </label>
                        <p className="pl-1">或拖拽文件到此处</p>
                      </div>
                      <p className="text-xs text-gray-500">支持 PDF, DOC, JPG, PNG 等格式, 最大 100MB</p>
                    </div>
                  </div>
                </div>
                <div className="form-control">
                  <label htmlFor="file-description" className="block text-sm font-medium text-gray-700">
                    文件描述 (可选)
                  </label>
                  <textarea
                    id="file-description"
                    className="textarea textarea-bordered mt-1"
                    placeholder="例如: 2023年第四季度财务报表"
                  ></textarea>
                </div>
                <div className="card-actions justify-end">
                  <button className="btn btn-primary btn-lg">提交存证</button>
                </div>
              </div>
            )}

            {activeTab === "hash" && (
              <div className="space-y-6">
                <div>
                  <h2 className="text-xl font-semibold">哈希上传存证</h2>
                  <p className="text-gray-500 mt-1">如果您已在本地计算好文件哈希，可直接提交哈希值进行存证。</p>
                </div>
                <div className="form-control">
                  <label htmlFor="hash-input" className="block text-sm font-medium text-gray-700">
                    文件哈希 (SHA256)
                  </label>
                  <input
                    id="hash-input"
                    type="text"
                    placeholder="请输入 64 位的 SHA256 哈希值"
                    className="input input-bordered w-full mt-1 font-mono"
                  />
                </div>
                <div className="form-control">
                  <label htmlFor="hash-description" className="block text-sm font-medium text-gray-700">
                    文件描述 (可选)
                  </label>
                  <textarea
                    id="hash-description"
                    className="textarea textarea-bordered mt-1"
                    placeholder="例如: 原始文件名或相关业务编号"
                  ></textarea>
                </div>
                <div className="card-actions justify-end">
                  <button className="btn btn-primary btn-lg">提交存证</button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Upload;
