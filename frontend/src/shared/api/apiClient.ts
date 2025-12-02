// src/shared/api/apiClient.ts
import axios from "axios";
import type { AxiosInstance } from "axios";
import { useAuthStore } from "@/store/useAuthStore";

// 🌐 Base URL for your backend
// Set VITE_API_URL in .env (e.g. http://localhost:8085/api)
const API_BASE_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8085/api";

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // ✅ send/receive cookies (refresh token)
  headers: {
    "Content-Type": "application/json",
  },
});

// ================================
// 🔐 Request interceptor (attach access token)
// ================================



function getAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}

apiClient.interceptors.request.use(
  (config) => {
    const token = getAccessToken();

    // public endpoints that DON'T need Authorization
    const isPublic =
      config.url?.startsWith("/auth/login") ||
      config.url?.startsWith("/auth/register") ||
      config.url?.startsWith("/auth/refresh") ||
      config.url?.startsWith("/health");

    if (!isPublic && token) {
      config.headers = config.headers ?? {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// ================================
// 🧯 Basic response interceptor (just logging for now)
// (later we can upgrade this into full refresh-token logic)
// ================================
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.warn(
      "[API ERROR]",
      error?.response?.status,
      error?.config?.url,
      error?.response?.data
    );
    return Promise.reject(error);
  }
);