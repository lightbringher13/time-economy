// changeEmailApi.types.ts

export type EmailChangeStatus =
  | "PENDING"
  | "NEW_EMAIL_VERIFIED"
  | "SECOND_FACTOR_PENDING"
  | "READY_TO_COMMIT"
  | "COMPLETED"
  | "CANCELED"
  | "EXPIRED";

export type SecondFactorType = "PHONE" | "OLD_EMAIL";

// ---- 1) Request email change ----
export interface RequestEmailChangeRequestDto {
  currentPassword: string;
  newEmail: string;
}

export interface RequestEmailChangeResponseDto {
  requestId: number;
  maskedNewEmail: string;
  status: EmailChangeStatus;
}

// ---- 2) Verify new-email code ----
export interface VerifyNewEmailCodeRequestDto {
  requestId: number;
  code: string;
}

export interface VerifyNewEmailCodeResponseDto {
  requestId: number;
  status: EmailChangeStatus;
}

// ---- 3) Start second factor ----
export interface StartSecondFactorRequestDto {
  requestId: number;
}

export interface StartSecondFactorResponseDto {
  requestId: number;
  secondFactorType: SecondFactorType;
  status: EmailChangeStatus;
}

// ---- 4) Verify second factor ----
export interface VerifySecondFactorRequestDto {
  requestId: number;
  code: string;
}

export interface VerifySecondFactorResponseDto {
  requestId: number;
  status: EmailChangeStatus;
}

// ---- 5) Commit ----
export interface CommitEmailChangeRequestDto {
  requestId: number;
}

export interface CommitEmailChangeResponseDto {
  requestId: number;
  newEmail: string;
  status: EmailChangeStatus;
}

// ---- 6) Get status (poll/resume) ----
export interface GetEmailChangeStatusResponseDto {
  requestId: number;
  status: EmailChangeStatus;
  secondFactorType: SecondFactorType | null;
  maskedNewEmail: string | null;
  expiresAt: string | null; // Instant ISO string
}