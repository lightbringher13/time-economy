// src/features/auth/register/hooks/useSendSignupOtp.ts
import { useCallback, useState } from "react";
import { sendSignupOtpApi } from "../api/signupApi";
import type { SendSignupOtpRequestDto, SendSignupOtpResponseDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useSendSignupOtp() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const send = useCallback(async (payload: SendSignupOtpRequestDto): Promise<SendSignupOtpResponseDto> => {
    setLoading(true);
    setError(null);
    try {
      return await sendSignupOtpApi(payload);
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to send verification code.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { send, loading, error };
}