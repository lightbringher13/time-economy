// src/features/auth/api/authApi.ts
import { apiClient } from "@/shared/api/apiClient";
import type { LoginRequest,
   AuthResponse,
    RegisterRequest,
     RegisterResponse,
    SendEmailCodeRequest,
  SendEmailCodeResponse,
VerifyEmailCodeRequest,
VerifyEmailCodeResponse,
EmailVerificationStatusResponse,
SignupBootstrapResponse,
UpdateSignupProfileRequest,
RequestPhoneVerificationCodeRequest,
VerifyPhoneCodeRequest,
VerifyPhoneCodeResponse,
PasswordResetRequest,
PasswordResetConfirm,
ChangePasswordRequest
 } from "../types/auth";


export async function loginApi(data: LoginRequest): Promise<AuthResponse> {
  const res = await apiClient.post<AuthResponse>("/auth/login", data);
  return res.data;
}


export async function refreshApi(): Promise<AuthResponse> {
  const res = await apiClient.post<AuthResponse>("/auth/refresh");
  return res.data;
}

export async function registerApi(
  data: RegisterRequest
): Promise<RegisterResponse> {
  const res = await apiClient.post<RegisterResponse>("/auth/register", data);
  return res.data;
}

export async function logoutApi(): Promise<void> {
  await apiClient.post("/auth/logout");
}

export async function logoutAllApi(): Promise<void> {
  await apiClient.post("/auth/logout-all");
}

export async function sendEmailCodeApi(data: SendEmailCodeRequest) {
  const res = await apiClient.post<SendEmailCodeResponse>(
    "/auth/email/send-code",
    data
  );
  return res.data;
}

export async function verifyEmailCodeApi(
  data: VerifyEmailCodeRequest
) {
  const res = await apiClient.post<VerifyEmailCodeResponse>(
    "/auth/email/verify",
    data
  );
  return res.data;
}

export async function getEmailVerificationStatusApi(
  email: string
): Promise<EmailVerificationStatusResponse> {
  const res = await apiClient.get<EmailVerificationStatusResponse>(
    "/auth/email/status",
    { params: { email } }
  );
  return res.data;
}

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

export async function requestPhoneCodeApi(
  payload: RequestPhoneVerificationCodeRequest
): Promise<void> {
  await apiClient.post("/auth/phone/request-code", payload);
  // nothing to return; if it doesn't throw, it succeeded
}

export async function verifyPhoneCodeApi(
  payload: VerifyPhoneCodeRequest
): Promise<VerifyPhoneCodeResponse> {
  const res = await apiClient.post<VerifyPhoneCodeResponse>(
    "/auth/phone/verify",
    payload
  );
  return res.data;
}

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

export const changePasswordApi = async (
  payload: ChangePasswordRequest
): Promise<void> => {
  await apiClient.post("/auth/password/change", payload);
};