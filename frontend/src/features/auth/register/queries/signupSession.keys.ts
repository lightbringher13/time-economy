// src/features/auth/register/queries/signupSession.keys.ts
export const signupSessionKeys = {
  root: ["auth", "signupSession"] as const,

  // single source of truth from server
  status: () => [...signupSessionKeys.root, "status"] as const,
} as const;