"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowRight, FileText, Lock, Network, Scale, Shield, Zap } from "lucide-react";
import type { NextPage } from "next";
import { Button } from "~~/components/ui/button";
import { WavyBackground } from "~~/components/ui/wavy-background";
import { useAuth } from "~~/contexts/AuthContext";

const AnimatedCounter = ({ target, duration = 2000 }: { target: number; duration?: number }) => {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let startTime: number;
    const animateCount = (timestamp: number) => {
      if (!startTime) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / duration, 1);
      setCount(Math.floor(progress * target));
      if (progress < 1) {
        requestAnimationFrame(animateCount);
      }
    };
    requestAnimationFrame(animateCount);
  }, [target, duration]);

  return <span className="text-3xl font-bold text-blue-400">{count}</span>;
};

const FeatureCard = ({
  icon,
  title,
  description,
  href,
  color = "blue",
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  href: string;
  color?: "blue" | "purple" | "green";
}) => {
  const colorClasses: Record<"blue" | "purple" | "green", string> = {
    blue: "from-blue-500 to-cyan-500",
    purple: "from-purple-500 to-pink-500",
    green: "from-green-500 to-emerald-500",
  };

  return (
    <Link href={href} className="group">
      <div className="relative h-full min-h-[300px] p-6 rounded-2xl bg-black/30 backdrop-blur-sm border border-white/10 hover:border-white/20 transition-all duration-500 hover:scale-105 hover:shadow-2xl overflow-hidden">
        {/* Animated background */}
        <div
          className={`absolute inset-0 bg-gradient-to-br ${colorClasses[color]} opacity-10 group-hover:opacity-20 transition-opacity duration-500`}
        />

        {/* Grid pattern */}
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGcgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIiBzdHJva2U9IiNmZmYiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMSI+PHBhdGggZD0iTTAgMGg0MHY0MEgweiIvPjwvZz48L3N2Zz4=')] opacity-20" />

        {/* Content */}
        <div className="relative z-10 h-full flex flex-col">
          <div className="mb-6">
            <div
              className={`w-16 h-16 rounded-full bg-gradient-to-br ${colorClasses[color]} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300`}
            >
              {icon}
            </div>
          </div>

          <h3 className="text-2xl font-bold text-white mb-4 group-hover:text-blue-300 transition-colors duration-300">
            {title}
          </h3>

          <p className="text-gray-300 text-sm leading-relaxed mb-6 flex-grow">{description}</p>

          <div className="flex items-center text-blue-400 group-hover:text-blue-300 transition-colors duration-300">
            <span className="text-sm font-medium">开始使用</span>
            <ArrowRight className="ml-2 w-4 h-4 group-hover:translate-x-1 transition-transform duration-300" />
          </div>
        </div>

        {/* Floating particles */}
        {[...Array(6)].map((_, i) => (
          <div
            key={i}
            className="absolute w-1 h-1 bg-white rounded-full opacity-30 group-hover:opacity-60 transition-opacity duration-500"
            style={{
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              animation: `float ${3 + Math.random() * 2}s ease-in-out infinite`,
              animationDelay: `${Math.random() * 2}s`,
            }}
          />
        ))}
      </div>
    </Link>
  );
};

const StatCard = ({ value, label, icon }: { value: number; label: string; icon: React.ReactNode }) => (
  <div className="text-center p-6 rounded-xl bg-white/5 backdrop-blur-sm border border-white/10">
    <div className="mb-2">{icon}</div>
    <div className="text-2xl font-bold text-white mb-1">
      <AnimatedCounter target={value} />
    </div>
    <div className="text-sm text-gray-400">{label}</div>
  </div>
);

const Home: NextPage = () => {
  const { user } = useAuth();

  return (
    <div className="min-h-screen relative overflow-hidden bg-black">
      {/* Dynamic background - full page */}
      <div className="fixed inset-0 z-0">
        <WavyBackground
          colors={["#3b82f6", "#8b5cf6", "#06b6d4", "#10b981", "#f59e0b"]}
          waveWidth={50}
          backgroundFill="black"
          blur={10}
          speed="fast"
          waveOpacity={0.3}
          className="min-h-screen"
        >
          <div /> {/* Empty div to satisfy children requirement */}
        </WavyBackground>
      </div>

      {/* Content overlay */}
      <div className="relative z-10">
        {/* Hero Section - 根据登录状态为固定 Header 留出空间 */}
        <section className={`min-h-screen flex items-center justify-center px-6 ${user ? "pt-16" : "pt-0"}`}>
          <div className="max-w-7xl mx-auto text-center">
            <div className="mb-8">
              <div className="inline-flex items-center px-4 py-2 rounded-full bg-white/10 backdrop-blur-sm border border-white/20 mb-6">
                <Shield className="w-4 h-4 text-green-400 mr-2" />
                <span className="text-sm text-gray-300">安全可信 · 区块链存证 · 法律效力</span>
              </div>

              <h1 className="text-5xl md:text-7xl font-bold mb-6 leading-tight">
                <span className="block text-white mb-2">区块链存证系统</span>
                <span className="block text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400">
                  让数据不可篡改
                </span>
              </h1>

              <p className="text-xl text-gray-300 mb-8 max-w-3xl mx-auto leading-relaxed">
                基于区块链技术，为您的电子数据提供安全、可信、高效的存证服务。
                确保数据完整性，具备法律效力，让每一份数据都有迹可循。
              </p>

              <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
                <Button
                  size="lg"
                  className="bg-gradient-to-r from-blue-500 to-purple-500 hover:from-blue-600 hover:to-purple-600 text-white px-8 py-4 text-lg"
                >
                  <Zap className="w-5 h-5 mr-2" />
                  立即开始
                </Button>
                <Button
                  size="lg"
                  variant="outline"
                  className="border-white/20 text-white hover:bg-white/10 bg-white/5 backdrop-blur-sm px-8 py-4 text-lg"
                >
                  <FileText className="w-5 h-5 mr-2" />
                  了解更多
                </Button>
              </div>
            </div>
          </div>
        </section>

        {/* Statistics Section */}
        <section className="py-20 px-6 bg-gradient-to-b from-transparent to-black/20">
          <div className="max-w-7xl mx-auto">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              <StatCard value={15420} label="存证文件" icon={<FileText className="w-6 h-6 text-blue-400" />} />
              <StatCard value={8931} label="活跃用户" icon={<Network className="w-6 h-6 text-purple-400" />} />
              <StatCard value={99999} label="验证通过率%" icon={<Shield className="w-6 h-6 text-green-400" />} />
              <StatCard value={24} label="小时在线" icon={<Zap className="w-6 h-6 text-yellow-400" />} />
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="py-20 px-6">
          <div className="max-w-7xl mx-auto">
            <div className="text-center mb-16">
              <h2 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent mb-6">
                核心功能
              </h2>
              <p className="text-xl md:text-2xl text-gray-300 max-w-3xl mx-auto leading-relaxed">
                为您提供全方位的区块链存证解决方案
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <FeatureCard
                icon={<FileText className="w-8 h-8 text-white" />}
                title="文件存证"
                description="支持多种文件格式上传，自动计算哈希值，将数据指纹永久记录在区块链上，确保文件完整性和真实性。"
                href="/upload"
                color="blue"
              />

              <FeatureCard
                icon={<Shield className="w-8 h-8 text-white" />}
                title="存证验证"
                description="通过多种验证方式，快速确认存证数据的真实性和有效性，提供详细的验证报告和区块链证明。"
                href="/verify"
                color="purple"
              />

              <FeatureCard
                icon={<Scale className="w-8 h-8 text-white" />}
                title="历史记录"
                description="查看完整的历史存证记录，随时下载具有法律效力的存证证明，支持时间轴展示和详细查询。"
                href="/history"
                color="green"
              />
            </div>
          </div>
        </section>

        {/* Technology Section */}
        <section className="py-20 px-6 bg-gradient-to-b from-transparent to-black/20">
          <div className="max-w-7xl mx-auto">
            <div className="text-center mb-16">
              <h2 className="text-4xl md:text-5xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent mb-6">
                技术优势
              </h2>
              <p className="text-xl md:text-2xl text-gray-300 max-w-3xl mx-auto leading-relaxed">
                采用最新的区块链技术，确保您的数据安全无忧
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
              {[
                {
                  icon: <Lock className="w-6 h-6 text-white" />,
                  title: "不可篡改",
                  description: "基于区块链的不可篡改特性，确保数据一旦上链就无法被修改",
                  color: "from-red-500 to-orange-500",
                },
                {
                  icon: <Shield className="w-6 h-6 text-white" />,
                  title: "去中心化",
                  description: "分布式存储，避免单点故障，提高系统可靠性和可用性",
                  color: "from-blue-500 to-cyan-500",
                },
                {
                  icon: <Network className="w-6 h-6 text-white" />,
                  title: "全链路可追溯",
                  description: "完整的操作记录和审计日志，确保每一笔交易都有据可查",
                  color: "from-green-500 to-emerald-500",
                },
                {
                  icon: <Zap className="w-6 h-6 text-white" />,
                  title: "高效便捷",
                  description: "优化的存证流程，快速完成数据上链和验证操作",
                  color: "from-yellow-500 to-amber-500",
                },
                {
                  icon: <Scale className="w-6 h-6 text-white" />,
                  title: "法律效力",
                  description: "符合相关法律法规要求，具备完全的法律效力",
                  color: "from-purple-500 to-pink-500",
                },
                {
                  icon: <FileText className="w-6 h-6 text-white" />,
                  title: "隐私保护",
                  description: "支持哈希存证，保护原始数据隐私和安全",
                  color: "from-indigo-500 to-purple-500",
                },
              ].map((tech, index) => (
                <div
                  key={index}
                  className="group relative p-6 rounded-2xl bg-black/40 backdrop-blur-md border border-white/10 hover:border-white/30 transition-all duration-500 hover:scale-105 hover:shadow-2xl overflow-hidden"
                >
                  {/* Animated background */}
                  <div
                    className={`absolute inset-0 bg-gradient-to-br ${tech.color} opacity-5 group-hover:opacity-10 transition-opacity duration-500`}
                  />

                  {/* Grid pattern */}
                  <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGcgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIiBzdHJva2U9IiNmZmYiIHN0cm9rZS1vcGFjaXR5PSIwLjEiIHN0cm9rZS13aWR0aD0iMSI+PHBhdGggZD0iTTAgMGg0MHY0MEgweiIvPjwvZz48L3N2Zz4=')] opacity-10" />

                  {/* Content */}
                  <div className="relative z-10">
                    <div className="w-14 h-14 rounded-xl bg-gradient-to-br border border-white/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300 shadow-lg">
                      {tech.icon}
                    </div>
                    <h3 className="text-xl font-bold text-white mb-3 group-hover:text-blue-300 transition-colors duration-300">
                      {tech.title}
                    </h3>
                    <p className="text-gray-300 text-sm leading-relaxed">{tech.description}</p>
                  </div>

                  {/* Floating particles */}
                  {[...Array(4)].map((_, i) => (
                    <div
                      key={i}
                      className="absolute w-1 h-1 bg-white rounded-full opacity-20 group-hover:opacity-40 transition-opacity duration-500"
                      style={{
                        left: `${10 + Math.random() * 80}%`,
                        top: `${10 + Math.random() * 80}%`,
                        animation: `float ${2 + Math.random() * 2}s ease-in-out infinite`,
                        animationDelay: `${Math.random() * 2}s`,
                      }}
                    />
                  ))}
                </div>
              ))}
            </div>
          </div>
        </section>
      </div>

      {/* Global styles for animations */}
      <style jsx global>{`
        @keyframes float {
          0%,
          100% {
            transform: translateY(0px);
          }
          50% {
            transform: translateY(-10px);
          }
        }
      `}</style>
    </div>
  );
};

export default Home;
