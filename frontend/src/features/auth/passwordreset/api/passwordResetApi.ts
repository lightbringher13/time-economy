import { apiClient } from "@/shared/api/apiClient";

import type { PasswordResetRequest, PasswordResetConfirm } from "./passwordResetApi.types";

export const requestPasswordResetApi = async (
  payload: PasswordResetRequest
): Promise<void> => {
  await apiClient.post("/auth/password/forgot", payload);
};

export const confirmPasswordResetApi = async (
  token: string,
  payload: PasswordResetConfirm
): Promise<void> => {
  await apiClient.post(`/auth/password/reset?token=${token}`, payload);
};