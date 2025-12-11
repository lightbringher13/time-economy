import { apiClient } from "@/shared/api/apiClient";

import type { RequestEmailChangeRequestDto,
    RequestEmailChangeResponseDto,
    VerifyNewEmailCodeRequestDto,
    VerifyNewEmailCodeResponseDto,
    VerifySecondFactorRequestDto,
    VerifySecondFactorResponseDto
 } from "./changeEmail.types";

// If you already have a pre-configured axios instance:
// If not, you can do: import axios from "axios"; and use axios instead of apiClient.

// ---- Shared types ----
export type SecondFactorType = "PHONE" | "OLD_EMAIL";

/**
 * POST /api/auth/email-change/request
 * Body: { currentPassword, newEmail }
 * Headers: Authorization: Bearer <token> (gateway injects X-User-Id)
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
 * Body: { requestId, code }
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
 * POST /api/auth/email-change/verify-second-factor
 * Body: { requestId, code }
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