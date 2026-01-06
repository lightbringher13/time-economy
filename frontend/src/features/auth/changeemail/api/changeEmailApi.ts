// changeEmailApi.ts
import { apiClient } from "@/shared/api/apiClient";

import type {
  RequestEmailChangeRequestDto,
  RequestEmailChangeResponseDto,
  VerifyNewEmailCodeRequestDto,
  VerifyNewEmailCodeResponseDto,
  StartSecondFactorRequestDto,
  StartSecondFactorResponseDto,
  VerifySecondFactorRequestDto,
  VerifySecondFactorResponseDto,
  CommitEmailChangeRequestDto,
  CommitEmailChangeResponseDto,
  GetEmailChangeStatusResponseDto,
} from "./changeEmailApi.types";

/**
 * POST /api/auth/email-change/request
 */
export async function requestEmailChangeApi(
  payload: RequestEmailChangeRequestDto
): Promise<RequestEmailChangeResponseDto> {
  const res = await apiClient.post<RequestEmailChangeResponseDto>(
    "/auth/email-change/request",
    payload
  );
  return res.data;
}

/**
 * POST /api/auth/email-change/verify-new-email
 */
export async function verifyNewEmailCodeApi(
  payload: VerifyNewEmailCodeRequestDto
): Promise<VerifyNewEmailCodeResponseDto> {
  const res = await apiClient.post<VerifyNewEmailCodeResponseDto>(
    "/auth/email-change/verify-new-email",
    payload
  );
  return res.data;
}

/**
 * POST /api/auth/email-change/start-second-factor
 */
export async function startSecondFactorApi(
  payload: StartSecondFactorRequestDto
): Promise<StartSecondFactorResponseDto> {
  const res = await apiClient.post<StartSecondFactorResponseDto>(
    "/auth/email-change/start-second-factor",
    payload
  );
  return res.data;
}

/**
 * POST /api/auth/email-change/verify-second-factor
 */
export async function verifySecondFactorApi(
  payload: VerifySecondFactorRequestDto
): Promise<VerifySecondFactorResponseDto> {
  const res = await apiClient.post<VerifySecondFactorResponseDto>(
    "/auth/email-change/verify-second-factor",
    payload
  );
  return res.data;
}

/**
 * POST /api/auth/email-change/commit
 */
export async function commitEmailChangeApi(
  payload: CommitEmailChangeRequestDto
): Promise<CommitEmailChangeResponseDto> {
  const res = await apiClient.post<CommitEmailChangeResponseDto>(
    "/auth/email-change/commit",
    payload
  );
  return res.data;
}

/**
 * GET /api/auth/email-change/status?requestId=123
 * (Controller needs to expose this endpoint; we’ll update next.)
 */
export async function getEmailChangeStatusApi(
  requestId: number
): Promise<GetEmailChangeStatusResponseDto> {
  const res = await apiClient.get<GetEmailChangeStatusResponseDto>(
    `/auth/email-change/${requestId}/status`
  );
  return res.data;
}

export async function getActiveEmailChangeApi(
): Promise<GetEmailChangeStatusResponseDto> {
  const res = await apiClient.get<GetEmailChangeStatusResponseDto>(
    "/auth/email-change/active"
  );
  return res.data;
}