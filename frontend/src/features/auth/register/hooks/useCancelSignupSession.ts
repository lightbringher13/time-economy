// src/features/auth/register/hooks/useCancelSignupSession.ts
import { useCallback, useState } from "react";
import { cancelSignupSessionApi } from "../api/signupApi";
import type { CancelSignupSessionResponseDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useCancelSignupSession() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cancel = useCallback(async (): Promise<CancelSignupSessionResponseDto> => {
    setLoading(true);
    setError(null);
    try {
      return await cancelSignupSessionApi();
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to cancel signup session.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { cancel, loading, error };
}