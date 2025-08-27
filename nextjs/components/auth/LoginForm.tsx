"use client";

import React, { useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { LoginRequest } from "../../types/auth";
import { toast } from "react-hot-toast";

interface LoginFormProps {
  onSwitchToRegister: () => void;
}

export const LoginForm: React.FC<LoginFormProps> = ({ onSwitchToRegister }) => {
  const [formData, setFormData] = useState<LoginRequest>({
    username: "",
    password: "",
  });
  const [error, setError] = useState<string>("");
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    setError("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 表单验证
    if (!formData.username.trim()) {
      setError("请输入用户名");
      return;
    }
    if (!formData.password) {
      setError("请输入密码");
      return;
    }

    setIsLoading(true);
    setError("");

    try {
      await login(formData);
    } catch (err) {
      let errorMessage = "登录失败，请稍后重试";
      if (err instanceof Error) {
        errorMessage = err.message;
      }
      setError(errorMessage);
      toast.error(errorMessage, {
        position: "top-center",
        duration: 5000,
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="card bg-base-100 w-full max-w-md shadow-xl">
      <div className="card-body">
        <h2 className="card-title text-2xl mb-6 justify-center">用户登录</h2>

        {error && (
          <div className="alert alert-error mb-4 animate-fade-in">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="stroke-current shrink-0 h-6 w-6"
              fill="none"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <div>
              <h3 className="font-bold">登录遇到问题</h3>
              <span className="text-sm">{error}</span>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="form-control w-full max-w-xs mx-auto">
            <label className="label">
              <span className="label-text">用户名</span>
            </label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              className="input input-bordered w-full"
              placeholder="请输入用户名"
              required
              disabled={isLoading}
            />
          </div>

          <div className="form-control w-full max-w-xs mx-auto">
            <label className="label">
              <span className="label-text">密码</span>
            </label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="input input-bordered w-full"
              placeholder="请输入密码"
              required
              disabled={isLoading}
            />
          </div>

          <div className="form-control mt-6 w-full max-w-xs mx-auto">
            <button type="submit" className="btn btn-primary w-full" disabled={isLoading}>
              {isLoading ? <span className="loading loading-spinner"></span> : null}
              登录
            </button>
          </div>
        </form>

        <div className="divider">或</div>

        <div className="text-center">
          <button onClick={onSwitchToRegister} className="btn btn-ghost btn-sm" disabled={isLoading}>
            没有账号？立即注册
          </button>
        </div>
      </div>
    </div>
  );
};
