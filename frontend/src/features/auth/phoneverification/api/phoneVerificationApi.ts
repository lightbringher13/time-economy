import { apiClient } from "@/shared/api/apiClient";

import type { RequestPhoneVerificationCodeRequest,
    VerifyPhoneCodeRequest,
    VerifyPhoneCodeResponse } from "./phoneVerificationApi.types";

export async function requestPhoneCodeApi(
  payload: RequestPhoneVerificationCodeRequest
): Promise<void> {
  await apiClient.post("/auth/phone/request-code", payload);
  // nothing to return; if it doesn't throw, it succeeded
}

export async function verifyPhoneCodeApi(
  payload: VerifyPhoneCodeRequest
): Promise<VerifyPhoneCodeResponse> {
  const res = await apiClient.post<VerifyPhoneCodeResponse>(
    "/auth/phone/verify",
    payload
  );
  return res.data;
}