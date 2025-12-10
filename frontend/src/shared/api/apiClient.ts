// src/shared/api/apiClient.ts
import axios from "axios";
import type { AxiosInstance } from "axios";
import { useAuthStore } from "@/store/useAuthStore";

const API_BASE_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8085/api";

export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // refresh 쿠키 자동 포함
  headers: {
    "Content-Type": "application/json",
  },
});

/* =========================================================
    🔐 REQUEST INTERCEPTOR — ACCESS TOKEN 추가
========================================================= */

function getAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}

apiClient.interceptors.request.use(
  (config) => {
    const token = getAccessToken();

    if (token) {
      config.headers = config.headers ?? {};
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

/* =========================================================
    🔁 REFRESH TOKEN 동시 요청 방지 (Mutex)
========================================================= */

let isRefreshing = false;
let refreshPromise: Promise<any> | null = null;

/* =========================================================
    🔁 실제 /auth/refresh API 호출 함수
========================================================= */
async function performRefresh() {
  const { login, logout } = useAuthStore.getState();

  try {
    const res = await axios.post(
      `${API_BASE_URL}/auth/refresh`,
      {},
      { withCredentials: true }
    );

    const newAccessToken = res.data?.accessToken;

    if (!newAccessToken) throw new Error("No access token returned");

    // Zustand store에 새로운 access token 저장
    login(newAccessToken);

    return newAccessToken;
  } catch (err) {
    logout();
    throw err;
  }
}

/* =========================================================
    🧯 RESPONSE INTERCEPTOR — Silent Refresh 구현
========================================================= */
apiClient.interceptors.response.use(
  (response) => response,

  async (error) => {
    const originalRequest = error.config;

    const status = error?.response?.status;
    const code = error?.response?.data?.code;

    // Silent Refresh 대상이 아닌 경우
    if (status !== 401 || code !== "ACCESS_TOKEN_EXPIRED") {
      return Promise.reject(error);
    }

    // 이미 refresh 시도한 요청은 다시 시도하지 않음
    if (originalRequest._retry) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      // 🔁 refresh 중복 요청 방지
      if (!isRefreshing) {
        isRefreshing = true;
        refreshPromise = performRefresh()
          .catch((err) => {
            throw err;
          })
          .finally(() => {
            isRefreshing = false;
            refreshPromise = null;
          });
      }

      const newToken = await refreshPromise;

      // Authorization 헤더 갱신
      originalRequest.headers = originalRequest.headers ?? {};
      originalRequest.headers.Authorization = `Bearer ${newToken}`;

      // 원래 요청 재시도
      return apiClient(originalRequest);
    } catch (refreshErr) {
      // refresh 탈락 (쿠키 없음, 만료 등) → 강제 로그아웃
      useAuthStore.getState().logout();
      return Promise.reject(refreshErr);
    }
  }
);