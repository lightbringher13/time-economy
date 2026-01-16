// src/features/auth/register/api/signupApi.types.ts

export type SignupSessionState =
  | "DRAFT"
  | "EMAIL_OTP_SENT"
  | "EMAIL_VERIFIED"
  | "PHONE_OTP_SENT"
  | "PHONE_VERIFIED"
  | "PROFILE_PENDING"
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
export interface SendSignupOtpRequestDto {
  target: SignupVerificationTarget;
  destination: string; // email or phone
}

export type SendSignupOtpOutcome =
  | "SENT"
  | "ALREADY_VERIFIED"
  | "INVALID_DESTINATION"
  | "THROTTLED";

export interface SendSignupOtpResponseDto {
  outcome: SendSignupOtpOutcome;
  sent: boolean;

  sessionId: string;          // always present when outcome is SENT/ALREADY_VERIFIED
  sessionCreated: boolean;    // ✅ NEW

  challengeId: string | null;
  ttlMinutes: number;
  maskedDestination: string | null;

  emailVerified: boolean;
  phoneVerified: boolean;

  emailOtpPending: boolean;   // ✅ NEW
  phoneOtpPending: boolean;   // ✅ NEW

  state: SignupSessionState;
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
export interface CancelSignupSessionResponseDto {
  sessionId: string | null;
  state: SignupSessionState;
}

// ---------------------
// Profile
// ---------------------
export type SignupGender = "MALE" | "FEMALE" | "OTHER";
export type LocalDateString = string; // "YYYY-MM-DD"

export type UpdateSignupProfileRequestDto = {
  name: string;
  gender: SignupGender;
  birthDate: LocalDateString;
};