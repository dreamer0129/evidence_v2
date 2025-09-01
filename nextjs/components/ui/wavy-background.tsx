"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createNoise3D } from "simplex-noise";
import { cn } from "~~/lib/utils";

export const WavyBackground = ({
  children,
  className,
  containerClassName,
  colors,
  waveWidth,
  backgroundFill,
  blur = 10,
  speed = "fast",
  waveOpacity = 0.5,
  ...props
}: {
  children?: any;
  className?: string;
  containerClassName?: string;
  colors?: string[];
  waveWidth?: number;
  backgroundFill?: string;
  blur?: number;
  speed?: "slow" | "fast";
  waveOpacity?: number;
  [key: string]: any;
}) => {
  const noise = useCallback(createNoise3D(), []);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animationRef = useRef<number | null>(null);
  const stateRef = useRef({
    w: 0,
    h: 0,
    nt: 0,
    ctx: null as CanvasRenderingContext2D | null,
    canvas: null as HTMLCanvasElement | null,
  });

  const getSpeed = useCallback(() => {
    switch (speed) {
      case "slow":
        return 0.001;
      case "fast":
        return 0.002;
      default:
        return 0.001;
    }
  }, [speed]);

  const waveColors = useMemo(() => colors ?? ["#38bdf8", "#818cf8", "#c084fc", "#e879f9", "#22d3ee"], [colors]);

  const drawWave = useCallback(() => {
    const { ctx, w, h } = stateRef.current;
    if (!ctx) return;

    stateRef.current.nt += getSpeed();

    for (let i = 0; i < 5; i++) {
      ctx.beginPath();
      ctx.lineWidth = waveWidth || 50;
      ctx.strokeStyle = waveColors[i % waveColors.length];

      for (let x = 0; x < w; x += 5) {
        const y = noise(x / 800, 0.3 * i, stateRef.current.nt) * 100;
        ctx.lineTo(x, y + h * 0.5);
      }

      ctx.stroke();
      ctx.closePath();
    }
  }, [waveWidth, waveColors, getSpeed, noise]);

  const render = useCallback(() => {
    const { ctx, w, h } = stateRef.current;
    if (!ctx) return;

    ctx.fillStyle = backgroundFill || "black";
    ctx.globalAlpha = waveOpacity || 0.5;
    ctx.fillRect(0, 0, w, h);

    drawWave();

    animationRef.current = requestAnimationFrame(render);
  }, [backgroundFill, waveOpacity, drawWave]);

  const init = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    stateRef.current.canvas = canvas;
    stateRef.current.ctx = ctx;
    stateRef.current.w = ctx.canvas.width = window.innerWidth;
    stateRef.current.h = ctx.canvas.height = window.innerHeight;
    stateRef.current.nt = 0;

    ctx.filter = `blur(${blur}px)`;

    const handleResize = () => {
      if (!stateRef.current.ctx) return;
      stateRef.current.w = stateRef.current.ctx.canvas.width = window.innerWidth;
      stateRef.current.h = stateRef.current.ctx.canvas.height = window.innerHeight;
      stateRef.current.ctx.filter = `blur(${blur}px)`;
    };

    window.onresize = handleResize;
    render();
  }, [blur, render]);

  useEffect(() => {
    init();
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [init]);

  const [isSafari, setIsSafari] = useState(false);
  useEffect(() => {
    // I'm sorry but i have got to support it on safari.
    setIsSafari(
      typeof window !== "undefined" &&
        navigator.userAgent.includes("Safari") &&
        !navigator.userAgent.includes("Chrome"),
    );
  }, []);

  return (
    <div className={cn("h-screen flex flex-col items-center justify-center", containerClassName)}>
      <canvas
        className="absolute inset-0 z-0"
        ref={canvasRef}
        id="canvas"
        style={{
          ...(isSafari ? { filter: `blur(${blur}px)` } : {}),
        }}
      ></canvas>
      <div className={cn("relative z-10", className)} {...props}>
        {children}
      </div>
    </div>
  );
};
