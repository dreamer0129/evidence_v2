"use client";

import type { NextPage } from "next";
import { Gallery4, Gallery4Item } from "~~/components/blocks/gallery4";

const FeatureCards = () => {
  const featureItems: Gallery4Item[] = [
    {
      id: "file-upload",
      title: "文件存证",
      description: "通过文件上传或哈希上传的方式进行存证",
      href: "/upload",
      image:
        "https://images.unsplash.com/photo-1618044619888-009e412ff12a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    },
    {
      id: "verification",
      title: "存证验证",
      description: "通过多种方式快速验证存证数据的真实性和有效性",
      href: "/verify",
      image:
        "https://images.unsplash.com/photo-1560472354-b33ff0c44a43?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    },
    {
      id: "history",
      title: "历史记录",
      description: "查看您的历史存证记录并下载存证证明",
      href: "/history",
      image:
        "https://images.unsplash.com/photo-1551288049-bebda4e38f71?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    },
  ];

  return (
    <div className="grow bg-base-300 w-full">
      <Gallery4 title="主要功能" description="区块链存证系统为您提供安全可靠的数据存证解决方案" items={featureItems} />
    </div>
  );
};

const InfoCards = () => {
  return (
    <>
      <div className="flex items-center flex-col pt-6 pb-4">
        <div className="px-5">
          <h1 className="text-center">
            <span className="block text-xl mb-1">欢迎使用</span>
            <span className="block text-3xl font-bold">区块链存证系统</span>
          </h1>
          <p className="text-center text-base mt-2">一个安全、可信、高效的电子数据存证平台</p>
        </div>
      </div>
    </>
  );
};

const Home: NextPage = () => {
  return (
    <>
      <InfoCards />

      <FeatureCards />
    </>
  );
};

export default Home;
