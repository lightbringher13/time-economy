import { apiClient } from "@/shared/api/apiClient";

import type { SendEmailCodeRequest,
    SendEmailCodeResponse,
    VerifyEmailCodeRequest,
    VerifyEmailCodeResponse,
    EmailVerificationStatusResponse,
 } from "./emailVerificationApi.types";

export async function sendEmailCodeApi(data: SendEmailCodeRequest) {
  const res = await apiClient.post<SendEmailCodeResponse>(
    "/auth/email/send-code",
    data
  );
  return res.data;
}

export async function verifyEmailCodeApi(
  data: VerifyEmailCodeRequest
) {
  const res = await apiClient.post<VerifyEmailCodeResponse>(
    "/auth/email/verify",
    data
  );
  return res.data;
}

export async function getEmailVerificationStatusApi(
  email: string
): Promise<EmailVerificationStatusResponse> {
  const res = await apiClient.get<EmailVerificationStatusResponse>(
    "/auth/email/status",
    { params: { email } }
  );
  return res.data;
}