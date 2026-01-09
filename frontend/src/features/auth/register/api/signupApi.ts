import { apiClient } from "@/shared/api/apiClient";
import type {
  SignupBootstrapResponseDto,
  SignupStatusResponseDto,
  SendSignupOtpRequestDto,
  SendSignupOtpResponseDto,
  ResendSignupOtpRequestDto,
  ResendSignupOtpResponseDto,
  VerifySignupOtpRequestDto,
  VerifySignupOtpResponseDto,
  EditSignupEmailRequestDto,
  EditSignupPhoneRequestDto,
  CancelSignupSessionResponseDto,
  UpdateSignupProfileRequestDto
} from "@/features/auth/register/api/signupApi.types"

export async function signupBootstrapApi(): Promise<SignupBootstrapResponseDto> {
  const res = await apiClient.get<SignupBootstrapResponseDto>("/auth/signup/bootstrap");
  return res.data;
}

export async function getSignupStatusApi(): Promise<SignupStatusResponseDto> {
  const res = await apiClient.get<SignupStatusResponseDto>("/auth/signup/status");
  return res.data;
}

export async function sendSignupOtpApi(
  payload: SendSignupOtpRequestDto
): Promise<SendSignupOtpResponseDto> {
  const res = await apiClient.post<SendSignupOtpResponseDto>("/auth/signup/send-otp", payload);
  return res.data;
}

export async function resendSignupOtpApi(
  payload: ResendSignupOtpRequestDto
): Promise<ResendSignupOtpResponseDto> {
  const res = await apiClient.post<ResendSignupOtpResponseDto>("/auth/signup/resend-otp", payload);
  return res.data;
}

export async function verifySignupOtpApi(
  payload: VerifySignupOtpRequestDto
): Promise<VerifySignupOtpResponseDto> {
  const res = await apiClient.post<VerifySignupOtpResponseDto>("/auth/signup/verify-otp", payload);
  return res.data;
}

export async function editSignupEmailApi(payload: EditSignupEmailRequestDto) {
  const res = await apiClient.post("/auth/signup/edit-email", payload);
  return res.data;
}

export async function editSignupPhoneApi(payload: EditSignupPhoneRequestDto) {
  const res = await apiClient.post("/auth/signup/edit-phone", payload);
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