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