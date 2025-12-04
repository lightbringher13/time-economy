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