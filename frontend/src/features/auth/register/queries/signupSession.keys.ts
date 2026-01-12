// src/features/auth/register/queries/signupSession.keys.ts
export const signupSessionKeys = {
  root: ["auth", "signupSession"] as const,
  status: () => [...signupSessionKeys.root, "status"] as const,
};