// 👉 Body you send to /auth/login
export interface LoginRequest {
  email: string;
  password: string;
}

export type RegisterRequest = {
  email: string;
  password: string;
  phoneNumber: string;
  name: string;
  gender: string;        // "MALE" | "FEMALE" | "OTHER" | etc. (depending on UI)
  birthDate: string;     // "yyyy-MM-dd"
};

export type RegisterResponse = {
  userId: number;
  email: string;
};

// 👉 What backend returns from /auth/login and /auth/refresh
export interface AuthResponse {
  accessToken: string;
}

export type ApiErrorResponse = {
  code: string;
  message: string;
};

export type SendEmailCodeRequest = {
  email: string;
};

export type SendEmailCodeResponse = {
  code: string;
};

export type VerifyEmailCodeRequest = {
  email: string;
  code: string;
};

export type VerifyEmailCodeResponse = {
  verified: boolean;
};

export type EmailVerificationStatusResponse = {
  verified: boolean;
};

// ✅ NEW: signup bootstrap response
export type SignupBootstrapResponse = {
  hasSession: boolean;
  email: string | null;
  emailVerified: boolean;
  phoneNumber: string | null;
  phoneVerified: boolean;
  name: string | null;
  gender: string | null;
  birthDate: string | null; // ISO yyyy-MM-dd from backend
  state: string | null;     // e.g. "EMAIL_PENDING", "EMAIL_VERIFIED"
};

export type UpdateSignupProfileRequest = {
  email: string;                // must not be null once autosave is triggered
  phoneNumber: string | null;
  name: string | null;
  gender: string | null;
  birthDate: string | null;     // yyyy-MM-dd
};

// Phone verification request/response types

export type RequestPhoneVerificationCodeRequest = {
  phoneNumber: string;
  countryCode?: string; // optional, backend defaults to +82
};

export type VerifyPhoneCodeRequest = {
  phoneNumber: string;
  code: string;
};

export type VerifyPhoneCodeResponse = {
  success: boolean;
};

// "비밀번호 재설정 메일 보내기" 폼
export type PasswordResetRequest = {
  email: string;
};

// "비밀번호 재설정" 폼 (토큰은 URL에서 가져옴)
export type PasswordResetConfirm = {
  newPassword: string;
  confirmPassword: string;
};

// 실제 BE에 보내는 DTO
export type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
};