// src/features/auth/register/hooks/useSignupBootstrap.ts
import { useCallback, useState } from "react";
import { signupBootstrapApi } from "../api/signupApi";
import type { SignupBootstrapResponseDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useSignupBootstrap() {
  const [data, setData] = useState<SignupBootstrapResponseDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const bootstrap = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const dto = await signupBootstrapApi();
      setData(dto);
      return dto;
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to bootstrap signup session.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { data, loading, error, bootstrap, setData };
}