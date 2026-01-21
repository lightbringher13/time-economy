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
}

// ---------------------
// Send OTP
// ---------------------
// src/features/auth/register/api/signupApi.types.ts

// ✅ include the new outcomes
export type SendSignupOtpOutcome =
  | "SENT"
  | "ALREADY_VERIFIED"
  | "INVALID_DESTINATION"
  | "THROTTLED"
  | "EMAIL_ALREADY_USED"
  | "PHONE_ALREADY_USED";

// ✅ sessionId is not always guaranteed on invalid input / duplicates,
// so make it nullable (safer and matches reality)
export interface SendSignupOtpResponseDto {
  outcome: SendSignupOtpOutcome;
  sent: boolean;

  sessionId: string | null;
  sessionCreated: boolean;

  challengeId: string | null;
  ttlMinutes: number;
  maskedDestination: string | null;

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
export interface VerifySignupOtpRequestDto {
  target: SignupVerificationTarget;
  code: string;
}

export type VerifySignupOtpOutcome =
  | "VERIFIED"
  | "WRONG_CODE"
  | "NO_SESSION"
  | "NO_PENDING_OTP"
  | "INVALID_INPUT";

export interface VerifySignupOtpResponseDto {
  outcome: VerifySignupOtpOutcome;
  success: boolean;

  sessionId: string | null;

  emailVerified: boolean;
  phoneVerified: boolean;

  emailOtpPending: boolean;
  phoneOtpPending: boolean;

  state: SignupSessionState;
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