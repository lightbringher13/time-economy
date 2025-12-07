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

// ✅ NEW: update signup profile request
export type UpdateSignupProfileRequest = {
  name: string | null;
  phoneNumber: string | null;
  gender: string | null;
  birthDate: string | null; // send as yyyy-MM-dd string
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

export type RegisterFormValues = {
  email: string;
  emailCode: string;
  password: string;
  passwordConfirm: string;
  phoneNumber: string;
  phoneCode: string;
  name: string;
  gender: "" | "MALE" | "FEMALE" | "OTHER";
  birthDate: string; // yyyy-MM-dd
};