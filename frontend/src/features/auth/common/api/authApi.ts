// src/features/auth/api/authApi.ts
import { apiClient } from "@/shared/api/apiClient";
import type { AuthResponse } from "../types/auth";

export async function refreshApi(): Promise<AuthResponse> {
  const res = await apiClient.post<AuthResponse>("/auth/refresh");
  return res.data;
}

export async function logoutApi(): Promise<void> {
  await apiClient.post("/auth/logout");
}

export async function logoutAllApi(): Promise<void> {
  await apiClient.post("/auth/logout-all");
}