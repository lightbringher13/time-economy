import { apiClient } from "@/shared/api/apiClient";

import type {
  SignupBootstrapResponse,
  UpdateSignupProfileRequest,
  VerifySignupOtpRequest,
  VerifySignupOtpResponse,
  SendSignupOtpRequest,
  SendSignupOtpResponse
} from "./signupSessionApi.types";

// ✅ NEW: bootstrap signup session
export const signupBootstrapApi = async (): Promise<SignupBootstrapResponse> => {
  const res = await apiClient.get<SignupBootstrapResponse>("/auth/signup/bootstrap");
  return res.data;
};

// ✅ NEW: autosave profile to signup session
export const updateSignupProfileApi = async (
  payload: UpdateSignupProfileRequest
): Promise<void> => {
  await apiClient.patch("/auth/signup/profile", payload);
};

// ✅ NEW: verify signup OTP (email/phone) + update signup session state
export const verifySignupOtpApi = async (
  payload: VerifySignupOtpRequest
): Promise<VerifySignupOtpResponse> => {
  const res = await apiClient.post<VerifySignupOtpResponse>(
    "/auth/signup/verify-otp",
    payload
  );
  return res.data;
};

// ✅ NEW: send signup OTP (email/phone)
export const sendSignupOtpApi = async (
  payload: SendSignupOtpRequest
): Promise<SendSignupOtpResponse> => {
  const res = await apiClient.post<SendSignupOtpResponse>(
    "/auth/signup/send-otp",
    payload
  );
  return res.data;
};
