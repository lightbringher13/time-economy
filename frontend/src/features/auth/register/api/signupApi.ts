// src/features/auth/register/api/signupApi.ts
import { apiClient } from "@/shared/api/apiClient";
import type {
  SignupStatusResponseDto,
  SendSignupOtpRequestDto,
  SendSignupOtpResponseDto,
  VerifySignupOtpRequestDto,
  VerifySignupOtpResponseDto,
  CancelSignupSessionResponseDto,
  UpdateSignupProfileRequestDto,
} from "@/features/auth/register/api/signupApi.types"

// ✅ bootstrap is removed in big-co version (lazy-open in send-otp)
// export async function signupBootstrapApi() { ... }

export async function getSignupStatusApi(): Promise<SignupStatusResponseDto> {
  const res = await apiClient.get<SignupStatusResponseDto>("/auth/signup/status");
  return res.data;
}

export async function sendSignupOtpApi(
  payload: SendSignupOtpRequestDto
): Promise<SendSignupOtpResponseDto> {
  const res = await apiClient.post<SendSignupOtpResponseDto>(
    "/auth/signup/send-otp",
    payload
  );
  return res.data;
}

export async function verifySignupOtpApi(
  payload: VerifySignupOtpRequestDto
): Promise<VerifySignupOtpResponseDto> {
  // ✅ cookie-only API: DO NOT send sessionId in body
  const res = await apiClient.post<VerifySignupOtpResponseDto>(
    "/auth/signup/verify-otp",
    payload
  );
  return res.data;
}

export async function cancelSignupSessionApi(): Promise<CancelSignupSessionResponseDto> {
  const res = await apiClient.post<CancelSignupSessionResponseDto>("/auth/signup/cancel");
  return res.data;
}

export async function updateSignupProfileApi(
  payload: UpdateSignupProfileRequestDto
): Promise<void> {
  await apiClient.patch("/auth/signup/profile", payload);
}