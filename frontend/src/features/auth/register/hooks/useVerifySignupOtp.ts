// src/features/auth/register/hooks/useVerifySignupOtp.ts
import { useCallback, useState } from "react";
import { verifySignupOtpApi } from "../api/signupApi";
import type { VerifySignupOtpRequestDto, VerifySignupOtpResponseDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useVerifySignupOtp() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const verify = useCallback(async (payload: VerifySignupOtpRequestDto): Promise<VerifySignupOtpResponseDto> => {
    setLoading(true);
    setError(null);
    try {
      return await verifySignupOtpApi(payload);
    } catch (e: any) {
      const msg = extractApiMessage(e, "Invalid or expired verification code.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { verify, loading, error };
}