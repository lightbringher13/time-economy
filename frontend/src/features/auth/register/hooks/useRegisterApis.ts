import { useCallback, useState } from "react";
import { extractApiMessage } from "./extractApiMessage";
import { registerApi } from "../api/registerApi";
import type { RegisterRequest, RegisterResponse } from "../api/registerApi.types";

export function useRegisterApis() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const register = useCallback(async (payload: RegisterRequest): Promise<RegisterResponse> => {
    setLoading(true);
    setError(null);
    try {
      return await registerApi(payload);
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to resend verification code.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { register, loading, error };
}