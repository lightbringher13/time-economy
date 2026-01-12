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

export interface SignupBootstrapResponseDto {
  exists: boolean;
  email: string | null;
  emailVerified: boolean;
  phoneNumber: string | null;
  phoneVerified: boolean;
  name: string | null;
  gender: string | null;
  birthDate: string | null; // yyyy-mm-dd
  state: SignupSessionState | null;
}

export interface SignupStatusResponseDto extends SignupBootstrapResponseDto {}

export interface SendSignupOtpRequestDto {
  target: SignupVerificationTarget;
}
export interface SendSignupOtpResponseDto {
  sent: boolean;
  ttlMinutes: number;
  maskedDestination: string | null;
  emailVerified: boolean;
  phoneVerified: boolean;
  state: SignupSessionState;
}

export interface VerifySignupOtpRequestDto {
  target: SignupVerificationTarget;
  code: string;
  sessionId?: string; // optional if your API accepts cookie-only
}
export interface VerifySignupOtpResponseDto {
  success: boolean;
  emailVerified: boolean;
  phoneVerified: boolean;
  state: string; // or SignupSessionState if you return it
}

export interface EditSignupEmailRequestDto {
  newEmail: string;
}
export interface EditSignupPhoneRequestDto {
  newPhoneNumber: string;
}

export interface CancelSignupSessionResponseDto {
  state: SignupSessionState;
}

export type SignupGender = "MALE" | "FEMALE" | "OTHER";

// BE expects LocalDate. In FE we send "YYYY-MM-DD" and let Jackson parse it.
export type LocalDateString = string;

export type UpdateSignupProfileRequestDto = {
  email?: string | null;        // optional: you can send null if you don’t want to update
  name: string;
  phoneNumber?: string | null;  // optional (but your BE currently accepts it)
  gender: SignupGender;
  birthDate: LocalDateString;   // "YYYY-MM-DD"
};