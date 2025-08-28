import { AuthResponse, LoginRequest, RegisterRequest, User } from "../types/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

class AuthService {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;

    try {
      const response = await fetch(url, {
        headers: {
          "Content-Type": "application/json",
          ...options.headers,
        },
        ...options,
      });

      if (!response.ok) {
        let errorMessage = "请求失败";

        // 首先根据状态码设置默认错误信息
        switch (response.status) {
          case 400:
          case 401:
            errorMessage = "用户名或密码错误";
            break;
          case 403:
            errorMessage = "访问被拒绝";
            break;
          case 404:
            errorMessage = "请求的资源不存在";
            break;
          case 500:
            errorMessage = "服务器内部错误，请稍后重试";
            break;
          default:
            errorMessage = `请求失败 (状态码: ${response.status})`;
        }

        // 尝试从响应中获取更详细的错误信息
        try {
          const errorText = await response.text();
          if (errorText) {
            // 尝试解析JSON错误信息
            try {
              const errorJson = JSON.parse(errorText);
              if (errorJson.message) {
                errorMessage = errorJson.message;
              } else if (errorJson.error) {
                errorMessage = errorJson.error;
              }
            } catch {
              // 如果不是JSON，直接使用错误文本（如果比默认错误信息更有用）
              if (errorText.length < 100) {
                // 避免显示过长的HTML错误页面
                errorMessage = errorText;
              }
            }
          }
        } catch {
          // 保持默认的状态码错误信息
        }

        throw new Error(errorMessage);
      }

      return response.json();
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      // 网络错误或其他异常
      throw new Error("网络连接失败，请检查您的网络连接");
    }
  }

  async login(credentials: LoginRequest): Promise<AuthResponse> {
    return this.request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
    });
  }

  async register(userData: RegisterRequest): Promise<string> {
    const response = await this.request<{ message: string }>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(userData),
    });
    return response.message;
  }

  async getCurrentUser(token: string): Promise<User> {
    return this.request<User>("/api/auth/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  logout(): void {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  }

  storeAuthData(token: string, user: User): void {
    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(user));
  }

  getStoredToken(): string | null {
    return localStorage.getItem("token");
  }

  getStoredUser(): User | null {
    const userStr = localStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  }
}

export const authService = new AuthService();
