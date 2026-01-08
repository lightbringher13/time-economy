// src/features/auth/register/hooks/useSignupStatus.ts
import { useCallback, useState } from "react";
import { getSignupStatusApi } from "../api/signupApi";
import type { SignupStatusResponseDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useSignupStatus() {
  const [data, setData] = useState<SignupStatusResponseDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const dto = await getSignupStatusApi();
      setData(dto);
      return dto;
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to load signup status.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { data, loading, error, refresh, setData };
}