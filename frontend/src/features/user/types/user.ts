// src/features/user/types/user.ts

// ===============================
// ✅ User profile (GET /me response)
// ===============================
export type UserProfile = {
  id: number;
  nickname: string;
  email: string;
  timecoinBalance: number;
};

// ===============================
// ✅ Change password (PATCH /me/password request)
// ===============================
export type ChangePasswordRequest = {
  currentPassword: string;
  newPassword: string;
};

// ===============================
// ✅ Change nickname (PATCH /me/nickname request)
// ===============================
export type ChangeNicknameRequest = {
  nickname: string;
};

export type ChangeNicknameResponse = {
  nickname: string;
};

export type SessionInfo = {
  id: number;
  deviceInfo: string | null;
  ipAddress: string | null;
  userAgent: string | null;

  // BE sends ISO-8601 strings → treat as string in TS
  createdAt: string;
  lastUsedAt: string | null;
  expiresAt: string;

  revoked: boolean;
  current: boolean;
};
