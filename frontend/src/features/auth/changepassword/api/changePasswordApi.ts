import { apiClient } from "@/shared/api/apiClient";

import type { ChangePasswordRequest } from "./changePasswordApi.type";

export const changePasswordApi = async (
  payload: ChangePasswordRequest
): Promise<void> => {
  await apiClient.post("/auth/password/change", payload);
};