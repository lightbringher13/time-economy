// src/features/auth/register/hooks/useResendSignupOtp.ts
import { useCallback, useState } from "react";
import { resendSignupOtpApi } from "../api/signupApi";
import type { ResendSignupOtpRequestDto, ResendSignupOtpResponseDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useResendSignupOtp() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const resend = useCallback(async (payload: ResendSignupOtpRequestDto): Promise<ResendSignupOtpResponseDto> => {
    setLoading(true);
    setError(null);
    try {
      return await resendSignupOtpApi(payload);
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to resend verification code.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { resend, loading, error };
}