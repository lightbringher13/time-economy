import { apiClient } from "@/shared/api/apiClient";

import type { SignupBootstrapResponse,UpdateSignupProfileRequest } from "./signupSessionApi.types";

// ✅ NEW: bootstrap signup session
export const signupBootstrapApi = async (): Promise<SignupBootstrapResponse> => {
  const res = await apiClient.get<SignupBootstrapResponse>(
    "/auth/signup/bootstrap"
  );
  return res.data;
};

// ✅ NEW: autosave profile to signup session
export const updateSignupProfileApi = async (
  payload: UpdateSignupProfileRequest
): Promise<void> => {
  await apiClient.patch("/auth/signup/profile", payload);
};