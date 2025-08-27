"use client";

import Link from "next/link";
import type { NextPage } from "next";
import { ArrowUpTrayIcon, CheckBadgeIcon, ClockIcon } from "@heroicons/react/24/outline";

const Home: NextPage = () => {
  return (
    <>
      <div className="flex items-center flex-col grow pt-10">
        <div className="px-5">
          <h1 className="text-center">
            <span className="block text-2xl mb-2">欢迎使用</span>
            <span className="block text-4xl font-bold">区块链存证系统</span>
          </h1>
          <p className="text-center text-lg mt-4">一个安全、可信、高效的电子数据存证平台</p>
        </div>

        <div className="grow bg-base-300 w-full mt-16 px-8 py-12">
          <div className="flex justify-center items-center gap-12 flex-col md:flex-row">
            <div className="flex flex-col bg-base-100 px-10 py-10 text-center items-center max-w-xs rounded-3xl">
              <ArrowUpTrayIcon className="h-8 w-8 fill-secondary" />
              <p className="mt-2">
                通过
                <Link href="/upload" passHref className="link">
                  文件上传
                </Link>
                或
                <Link href="/upload" passHref className="link">
                  哈希上传
                </Link>
                的方式进行存证
              </p>
            </div>
            <div className="flex flex-col bg-base-100 px-10 py-10 text-center items-center max-w-xs rounded-3xl">
              <CheckBadgeIcon className="h-8 w-8 fill-secondary" />
              <p className="mt-2">
                通过
                <Link href="/verify" passHref className="link">
                  多种方式
                </Link>
                快速验证存证数据的真实性和有效性
              </p>
            </div>
            <div className="flex flex-col bg-base-100 px-10 py-10 text-center items-center max-w-xs rounded-3xl">
              <ClockIcon className="h-8 w-8 fill-secondary" />
              <p className="mt-2">
                查看您的
                <Link href="/history" passHref className="link">
                  历史存证记录
                </Link>
                并下载存证证明
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default Home;
