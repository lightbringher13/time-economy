// src/features/user/types/user.ts

// features/user/api/userApi.ts (혹은 기존 위치)

export type UserStatus = "ACTIVE" | "INACTIVE" | "BLOCKED" | string; 
// enum 값 정확히 알면 거기에 맞게 좁혀도 됨

export type UserProfile = {
  id: number;
  email: string;
  name: string;
  phoneNumber: string | null;
  birthDate: string | null;   // LocalDate → FE에서는 string (예: "1995-03-21")
  gender: string | null;      // "MALE" | "FEMALE" | "OTHER" | null 이런 식으로 나올 것
  status: UserStatus;
  createdAt: string;          // LocalDateTime → ISO string
  updatedAt: string;
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
