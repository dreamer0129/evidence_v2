"use client";

import React from "react";
import { motion } from "framer-motion";
import { cn } from "~~/lib/utils";

interface GlassContainerProps {
  children: React.ReactNode;
  className?: string;
  intensity?: "low" | "medium" | "high";
  animate?: boolean;
  hover?: boolean;
}

export const GlassContainer: React.FC<GlassContainerProps> = ({
  children,
  className = "",
  intensity = "medium",
  animate = false,
  hover = false,
}) => {
  const getGlassStyles = () => {
    const baseStyles = "backdrop-blur-md border border-white/20 shadow-lg";

    switch (intensity) {
      case "low":
        return cn(baseStyles, "bg-white/10 dark:bg-black/10");
      case "high":
        return cn(baseStyles, "bg-white/30 dark:bg-black/30");
      case "medium":
      default:
        return cn(baseStyles, "bg-white/20 dark:bg-black/20");
    }
  };

  const containerVariants = {
    initial: {
      opacity: 0,
      y: 20,
    },
    animate: {
      opacity: 1,
      y: 0,
    },
    hover: hover
      ? {
          y: -5,
        }
      : {},
  };

  return (
    <motion.div
      className={cn(
        "rounded-2xl relative overflow-hidden",
        getGlassStyles(),
        "before:absolute before:inset-0 before:bg-gradient-to-br before:from-white/10 before:to-transparent before:opacity-50",
        "after:absolute after:inset-0 after:bg-[radial-gradient(circle_at_center,transparent_0%,rgba(255,255,255,0.05)_100%)]",
        className,
      )}
      variants={containerVariants}
      initial={animate ? "initial" : undefined}
      animate={animate ? "animate" : undefined}
      whileHover={hover ? "hover" : undefined}
      transition={{ duration: 0.5, ease: "easeOut" }}
    >
      {/* Inner glow effect */}
      <div className="absolute inset-0 bg-gradient-to-br from-transparent via-white/5 to-transparent opacity-50" />

      {/* Subtle border gradient */}
      <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-white/20 via-transparent to-transparent opacity-30" />

      {/* Content */}
      <div className="relative z-10">{children}</div>
    </motion.div>
  );
};

// Specialized glass containers for different use cases
interface GlassCardProps {
  children: React.ReactNode;
  className?: string;
  animate?: boolean;
  intensity?: "low" | "medium" | "high";
}

export const GlassCard: React.FC<GlassCardProps> = ({
  children,
  className = "",
  animate = false,
  intensity = "medium",
}) => (
  <GlassContainer intensity={intensity} className={cn("p-6", className)} animate={animate} hover>
    {children}
  </GlassContainer>
);

interface GlassSectionProps {
  children: React.ReactNode;
  className?: string;
  animate?: boolean;
  intensity?: "low" | "medium" | "high";
}

export const GlassSection: React.FC<GlassSectionProps> = ({
  children,
  className = "",
  animate = false,
  intensity = "low",
}) => (
  <GlassContainer intensity={intensity} className={cn("p-8", className)} animate={animate}>
    {children}
  </GlassContainer>
);

export const GlassButton: React.FC<{
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
  disabled?: boolean;
}> = ({ children, className = "", onClick, disabled = false }) => (
  <motion.button
    className={cn(
      "relative px-6 py-3 rounded-xl font-medium text-white",
      "bg-gradient-to-r from-blue-500 to-purple-600",
      "hover:from-blue-600 hover:to-purple-700",
      "disabled:opacity-50 disabled:cursor-not-allowed",
      "shadow-lg hover:shadow-xl",
      "transition-all duration-300",
      "before:absolute before:inset-0 before:rounded-xl before:bg-white/20 before:opacity-0 hover:before:opacity-20",
      className,
    )}
    onClick={onClick}
    disabled={disabled}
    whileHover={{ scale: disabled ? 1 : 1.02 }}
    whileTap={{ scale: disabled ? 1 : 0.98 }}
  >
    <span className="relative z-10">{children}</span>
  </motion.button>
);
