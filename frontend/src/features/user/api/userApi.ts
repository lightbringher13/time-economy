// src/features/user/api/userApi.ts

import { apiClient } from "@/shared/api/apiClient";
import type {
  UserProfile,
  ChangePasswordRequest,
  ChangeNicknameRequest,
  ChangeNicknameResponse,
  SessionInfo,
  RegisterRequest
} from "../types/user";

// ===============================
// ✅ GET /me → fetch user profile
// ===============================
export async function getMeApi(): Promise<UserProfile> {
  const res = await apiClient.get<UserProfile>("/users/me");
  return res.data;
}

// ===============================
// ✅ GET /me/sessions → list sessions for current user
// ===============================
export async function getSessionsApi(): Promise<SessionInfo[]> {
  const res = await apiClient.get<SessionInfo[]>("/users/me/sessions");
  return res.data;
}

// ===============================
// ✅ DELETE /me/sessions/{id} → revoke one session
// ===============================
export async function revokeSessionApi(sessionId: number): Promise<void> {
  await apiClient.delete(`/users/me/sessions/${sessionId}`);
}

// ===============================
// ✅ PATCH /me/password → change password
// ===============================
export async function changePasswordApi(
  data: ChangePasswordRequest
): Promise<void> {
  await apiClient.patch("/users/me/password", data);
}

// ===============================
// ✅ PATCH /me/nickname → change nickname
// ===============================
export async function changeNicknameApi(
  data: ChangeNicknameRequest
): Promise<ChangeNicknameResponse> {
  const res = await apiClient.patch<ChangeNicknameResponse>(
    "/users/me/nickname",
    data
  );
  return res.data;
}

// ✅ NEW: register
export async function registerApi(
  data: RegisterRequest
): Promise<void> {
  // BE returns 201 with no body
  await apiClient.post("/auth/register", data);
}
