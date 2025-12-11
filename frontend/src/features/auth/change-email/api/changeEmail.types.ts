import type { SecondFactorType } from "./changeEmailApi";

// ---- 1) Request email change ----

export interface RequestEmailChangeRequestDto {
  currentPassword: string;
  newEmail: string;
}

export interface RequestEmailChangeResponseDto {
  requestId: number;
  maskedNewEmail: string;
}

// ---- 2) Verify new-email code ----

export interface VerifyNewEmailCodeRequestDto {
  requestId: number;
  code: string;
}

export interface VerifyNewEmailCodeResponseDto {
  requestId: number;
  secondFactorType: SecondFactorType;
}

// ---- 3) Verify second factor & commit ----

export interface VerifySecondFactorRequestDto {
  requestId: number;
  code: string;
}

export interface VerifySecondFactorResponseDto {
  requestId: number;
  newEmail: string;
}