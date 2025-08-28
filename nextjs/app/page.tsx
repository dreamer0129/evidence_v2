"use client";

import Link from "next/link";
import type { NextPage } from "next";
import { ArrowUpTrayIcon, CheckBadgeIcon, ClockIcon } from "@heroicons/react/24/outline";
import {
  AnimatedCard,
  CardBody,
  CardDescription,
  CardTitle,
  CardVisual,
} from "~~/components/ui/interactive-bento-grid";

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
          ;
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <AnimatedCard>
              <CardVisual>
                <div className="flex items-center justify-center h-full">
                  <ArrowUpTrayIcon className="h-16 w-16 text-primary" />
                </div>
              </CardVisual>
              <CardBody>
                <CardTitle>文件存证</CardTitle>
                <CardDescription>
                  通过
                  <Link href="/upload" className="link">
                    文件上传
                  </Link>
                  或
                  <Link href="/upload" className="link">
                    哈希上传
                  </Link>
                  的方式进行存证
                </CardDescription>
              </CardBody>
            </AnimatedCard>

            <AnimatedCard>
              <CardVisual>
                <div className="flex items-center justify-center h-full">
                  <CheckBadgeIcon className="h-16 w-16 text-primary" />
                </div>
              </CardVisual>
              <CardBody>
                <CardTitle>存证验证</CardTitle>
                <CardDescription>
                  通过
                  <Link href="/verify" className="link">
                    多种方式
                  </Link>
                  快速验证存证数据的真实性和有效性
                </CardDescription>
              </CardBody>
            </AnimatedCard>

            <AnimatedCard>
              <CardVisual>
                <div className="flex items-center justify-center h-full">
                  <ClockIcon className="h-16 w-16 text-primary" />
                </div>
              </CardVisual>
              <CardBody>
                <CardTitle>历史记录</CardTitle>
                <CardDescription>
                  查看您的
                  <Link href="/history" className="link">
                    历史存证记录
                  </Link>
                  并下载存证证明
                </CardDescription>
              </CardBody>
            </AnimatedCard>
          </div>
        </div>
      </div>
    </>
  );
};

export default Home;
