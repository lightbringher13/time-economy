// src/features/auth/register/api/signupApi.types.ts
export type SignupSessionState =
  | "DRAFT"
  | "EMAIL_OTP_SENT"
  | "EMAIL_VERIFIED"
  | "PHONE_OTP_SENT"
  | "PHONE_VERIFIED"
  | "PROFILE_READY"
  | "COMPLETED"
  | "CANCELED"
  | "EXPIRED";

export type SignupVerificationTarget = "EMAIL" | "PHONE";

// ---------------------
// Status
// ---------------------
export interface SignupStatusResponseDto {
  exists: boolean;

  email: string | null;
  emailVerified: boolean;

  phoneNumber: string | null;
  phoneVerified: boolean;

  name: string | null;
  gender: string | null;
  birthDate: string | null; // yyyy-mm-dd

  state: SignupSessionState;

  emailOtpPending: boolean;
  phoneOtpPending: boolean;

  // ✅ NEW (Instant -> ISO string)
  emailOtpExpiresAt: string | null;
  phoneOtpExpiresAt: string | null;
}

// ---------------------
// Send OTP
// ---------------------
export type SendSignupOtpOutcome =
  | "SENT"
  | "ALREADY_VERIFIED"
  | "INVALID_DESTINATION"
  | "INVALID_INPUT"
  | "INVALID_STATE"
  | "THROTTLED"
  | "DELIVERY_FAILED"
  | "EMAIL_ALREADY_USED"
  | "PHONE_ALREADY_USED";

export interface SendSignupOtpResponseDto {
  outcome: SendSignupOtpOutcome;
  sent: boolean;

  sessionId: string | null;
  sessionCreated: boolean;

  challengeId: string | null;

  // keep for backward compatibility (you can compute from expiresAt too)
  ttlMinutes: number;

  maskedDestination: string | null;

  // ✅ NEW (server truth)
  retryAfterSeconds: number | null;

  // ✅ NEW (Instant -> ISO string)
  emailOtpExpiresAt: string | null;
  phoneOtpExpiresAt: string | null;

  emailVerified: boolean;
  phoneVerified: boolean;

  emailOtpPending: boolean;
  phoneOtpPending: boolean;

  state: SignupSessionState;
}

export interface SendSignupOtpRequestDto {
  target: SignupVerificationTarget;
  destination: string;
}

// ---------------------
// Verify OTP
// ---------------------
export type VerifySignupOtpOutcome =
  | "VERIFIED"
  | "WRONG_CODE"
  | "NO_SESSION"
  | "NO_PENDING_OTP"
  | "INVALID_INPUT"
  // ✅ if you later bubble these up from verification:
  | "EXPIRED"
  | "LOCKED"
  | "THROTTLED";

export interface VerifySignupOtpResponseDto {
  outcome: VerifySignupOtpOutcome;
  success: boolean;

  sessionId: string | null;

  emailVerified: boolean;
  phoneVerified: boolean;

  emailOtpPending: boolean;
  phoneOtpPending: boolean;

  state: SignupSessionState;

  // ✅ optional future-proof fields (only if your BE returns them)
  expiresAt?: string | null;
  remainingAttempts?: number | null;
  retryAfterSeconds?: number | null;
}

export interface VerifySignupOtpRequestDto {
  target: SignupVerificationTarget;
  code: string;
}

// ---------------------
// Cancel
// ---------------------
// src/features/auth/register/api/signupApi.types.ts

export type CancelSignupSessionOutcome =
  | "CANCELED"
  | "ALREADY_TERMINAL"
  | "NO_SESSION"
  | "INVALID_INPUT";

export interface CancelSignupSessionResponseDto {
  outcome: CancelSignupSessionOutcome;
  canceled: boolean;

  sessionId: string;
  state: SignupSessionState;
}

// ---------------------
// Profile
// ---------------------
export type SignupGender = "MALE" | "FEMALE"; // keep in sync with BE + your zod enum
export type LocalDateString = string;

export type UpdateSignupProfileRequestDto = {
  name: string;
  gender: SignupGender;
  birthDate: LocalDateString;
};

export type UpdateSignupProfileOutcome =
  | "UPDATED"
  | "NO_SESSION"
  | "INVALID_INPUT"
  | "INVALID_STATE";

export type UpdateSignupProfileResponseDto = {
  outcome: UpdateSignupProfileOutcome;
  updated: boolean;
  sessionId: string;
  state: SignupSessionState;
};