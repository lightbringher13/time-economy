// src/features/auth/api/authApi.ts
import { apiClient } from "@/shared/api/apiClient";
import type { AxiosError } from "axios";
import type { LoginRequest, AuthResponse, ApiErrorResponse } from "../types/auth";

// 👉 Login API: POST /auth/login
export async function loginApi(data: LoginRequest): Promise<AuthResponse> {
  const res = await apiClient.post<AuthResponse>("/auth/login", data);
  return res.data;
}

// 👉 Refresh API: POST /auth/refresh
// - On success (normal or benign reuse handled in BE): 200 + { accessToken }
// - On suspicious reuse / invalid / expired: 401 + ApiErrorResponse
export async function refreshApi(): Promise<AuthResponse> {
  const res = await apiClient.post<AuthResponse>("/auth/refresh");
  return res.data;
}

// POST /auth/logout
// - logs out only the *current* session (current refresh token)
// - BE should clear refresh cookie for this device
export async function logoutApi(): Promise<void> {
  await apiClient.post("/auth/logout");
}

// POST /auth/logout-all
// - revokes all refresh tokens for this user (all devices)
// - BE clears refresh cookie only for *this* browser (others just become invalid)
export async function logoutAllApi(): Promise<void> {
  await apiClient.post("/auth/logout-all");
}

// 🔹 Error code from BE for suspicious reuse
// GlobalExceptionHandler sends:
//  code = "REFRESH_FAMILY_REVOKED"
export const REFRESH_FAMILY_REVOKED = "REFRESH_FAMILY_REVOKED";

// 🔍 Type guard: check if unknown error is an AxiosError<ApiErrorResponse>
export function isApiError(
  error: unknown
): error is AxiosError<ApiErrorResponse> {
  return (
    !!error &&
    typeof error === "object" &&
    "isAxiosError" in error &&
    (error as any).isAxiosError === true
  );
}