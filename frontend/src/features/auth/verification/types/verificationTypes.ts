export type VerificationChannel = "EMAIL" | "SMS";

// Keep this aligned with your backend enum names
export type VerificationPurpose =
  | "SIGNUP_EMAIL"
  | "SIGNUP_PHONE"
  | "PASSWORD_RESET"
  | "CHANGE_EMAIL_NEW"
  | "CHANGE_EMAIL_OLD"
  | "MFA_PHONE";

export type CreateOtpRequest = {
  purpose: VerificationPurpose;
  channel: VerificationChannel;
  destination: string; // email or phone
};

export type CreateOtpResponse = {
  challengeId: number;
  sent: boolean;
  ttlMinutes: number;
  maskedDestination: string;
};

export type VerifyOtpRequest = {
  purpose: VerificationPurpose;
  channel: VerificationChannel;
  destination: string;
  code: string;
};

export type VerifyOtpResponse = {
  success: boolean;
};

export type CreateLinkRequest = {
  purpose: VerificationPurpose;
  channel: VerificationChannel; // usually EMAIL
  destination: string;          // email
  linkBaseUrl: string;          // FE route, ex: https://app/reset-password
};

export type CreateLinkResponse = {
  challengeId: number;
  sent: boolean;
  ttlMinutes: number;
  maskedDestination: string;
};

export type VerifyLinkRequest = {
  purpose: VerificationPurpose;
  channel: VerificationChannel;
  token: string;
};

export type VerifyLinkResponse = {
  success: boolean;
  challengeId: number | null;
  destinationNorm: string | null; // normalized email (or phone later)
};