// 👉 Body you send to /auth/login
export interface LoginRequest {
  email: string;
  password: string;
}

// 👉 What backend returns from /auth/login and /auth/refresh
export interface LoginResponse {
  accessToken: string;
}