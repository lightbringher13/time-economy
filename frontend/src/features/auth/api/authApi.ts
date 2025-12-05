// src/features/auth/api/authApi.ts
import { apiClient } from "@/shared/api/apiClient";
import type { AxiosError } from "axios";
import type { LoginRequest,
   AuthResponse,
   ApiErrorResponse,
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
VerifyPhoneCodeResponse
 } from "../types/auth";

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

// 👉 Register API: POST /auth/refresh
export async function registerApi(
  data: RegisterRequest
): Promise<RegisterResponse> {
  const res = await apiClient.post<RegisterResponse>("/auth/register", data);
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