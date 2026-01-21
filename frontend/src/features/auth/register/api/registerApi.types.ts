// src/features/auth/register/api/registerApi.types.ts (or inside signupApi.types.ts)

export type RegisterRequestDto = {
  password: string;
};

export type RegisterOutcome =
  | "REGISTERED"
  | "NO_SESSION"
  | "SESSION_EXPIRED_OR_NOT_FOUND"
  | "INVALID_STATE"
  | "EMAIL_ALREADY_USED"
  | "PHONE_ALREADY_USED"
  | "INVALID_INPUT";

// What the controller returns (recommended: always include outcome)
export type RegisterResponseDto = {
  outcome: RegisterOutcome;
  registered: boolean;
  userId: number | null;
  email: string | null;
};