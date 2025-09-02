"use client";

import React from "react";
import { motion } from "framer-motion";
import { cn } from "~~/lib/utils";

interface GlassTabsProps {
  tabs: {
    id: string;
    label: string;
    icon?: React.ReactNode;
  }[];
  activeTab: string;
  onTabChange: (tabId: string) => void;
  className?: string;
}

export const GlassTabs: React.FC<GlassTabsProps> = ({ tabs, activeTab, onTabChange, className = "" }) => {
  return (
    <div className={cn("relative", className)}>
      {/* Tabs container */}
      <div className="flex space-x-1 p-1 bg-black/20 backdrop-blur-md rounded-xl border border-white/10">
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;

          return (
            <motion.button
              key={tab.id}
              className={cn(
                "relative px-6 py-3 rounded-lg text-sm font-medium transition-all duration-300 flex items-center space-x-2",
                isActive ? "text-white" : "text-gray-400 hover:text-white hover:bg-white/5",
              )}
              onClick={() => onTabChange(tab.id)}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              {/* Active tab background */}
              {isActive && (
                <motion.div
                  layoutId="activeTab"
                  className="absolute inset-0 bg-gradient-to-r from-blue-500/20 to-purple-500/20 rounded-lg border border-white/20"
                  initial={false}
                  animate={{ opacity: 1 }}
                  transition={{
                    type: "spring",
                    stiffness: 300,
                    damping: 30,
                  }}
                />
              )}

              {/* Active tab glow */}
              {isActive && (
                <motion.div
                  layoutId="activeTabGlow"
                  className="absolute inset-0 bg-gradient-to-r from-blue-500/10 to-purple-500/10 rounded-lg blur-md"
                  initial={false}
                  animate={{ opacity: 1 }}
                  transition={{
                    type: "spring",
                    stiffness: 300,
                    damping: 30,
                  }}
                />
              )}

              {/* Tab content */}
              <span className="relative z-10 flex items-center space-x-2">
                {tab.icon && <span className="w-4 h-4">{tab.icon}</span>}
                <span>{tab.label}</span>
              </span>
            </motion.button>
          );
        })}
      </div>
    </div>
  );
};
