"use client";

import React, { useState } from "react";
import { LoginForm } from "./LoginForm";
import { RegisterForm } from "./RegisterForm";

type AuthMode = "login" | "register";

export const AuthPage: React.FC = () => {
  const [mode, setMode] = useState<AuthMode>("login");

  const switchToRegister = () => setMode("register");
  const switchToLogin = () => setMode("login");

  return (
    <div className="min-h-screen bg-base-200 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-primary mb-2">区块链存证系统</h1>
          <p className="text-lg text-base-content">{mode === "login" ? "欢迎回来，请登录" : "创建新账户，开始使用"}</p>
        </div>

        {mode === "login" ? (
          <LoginForm onSwitchToRegister={switchToRegister} />
        ) : (
          <RegisterForm onSwitchToLogin={switchToLogin} />
        )}

        <div className="mt-8 text-center text-sm text-base-content/60">
          <p>© 2024 区块链存证系统. 保留所有权利.</p>
        </div>
      </div>
    </div>
  );
};
