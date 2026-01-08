// src/features/auth/register/hooks/useEditSignupEmail.ts
import { useCallback, useState } from "react";
import { editSignupEmailApi } from "../api/signupApi";
import type { EditSignupEmailRequestDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useEditSignupEmail() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const editEmail = useCallback(async (payload: EditSignupEmailRequestDto) => {
    setLoading(true);
    setError(null);
    try {
      return await editSignupEmailApi(payload);
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to update email.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { editEmail, loading, error };
}