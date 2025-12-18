// ✅ NEW: signup bootstrap response
export type SignupBootstrapResponse = {
  hasSession: boolean;
  email: string | null;
  emailVerified: boolean;
  phoneNumber: string | null;
  phoneVerified: boolean;
  name: string | null;
  gender: string | null;
  birthDate: string | null; // ISO yyyy-MM-dd from backend
  state: string | null;     // e.g. "EMAIL_PENDING", "EMAIL_VERIFIED"
};

export type UpdateSignupProfileRequest = {
  email: string;                // must not be null once autosave is triggered
  phoneNumber: string | null;
  name: string | null;
  gender: string | null;
  birthDate: string | null;     // yyyy-MM-dd
};

export type SignupVerificationTarget = "EMAIL" | "PHONE";

export type VerifySignupOtpRequest = {
  target: SignupVerificationTarget; // "EMAIL" | "PHONE"
  code: string;                     // 6 digits
};

export type VerifySignupOtpResponse = {
  success: boolean;
  sessionId: string;
  emailVerified: boolean;
  phoneVerified: boolean;
  state: string; // e.g. "EMAIL_VERIFIED", "PROFILE_FILLED", ...
};

// ✅ NEW: send signup OTP request/response
export type SendSignupOtpRequest = {
  target: SignupVerificationTarget; // "EMAIL" | "PHONE"
};

export type SendSignupOtpResponse = {
  sent: boolean;
  ttlMinutes: number;
  maskedDestination: string | null;
  emailVerified: boolean;
  phoneVerified: boolean;
  state: string; // "EMAIL_PENDING", ...
};

