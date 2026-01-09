// src/features/auth/register/hooks/useEditSignupPhone.ts
import { useCallback, useState } from "react";
import { editSignupPhoneApi } from "../api/signupApi";
import type { EditSignupPhoneRequestDto } from "../api/signupApi.types";
import { extractApiMessage } from "./extractApiMessage";

export function useEditSignupPhone() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const editPhone = useCallback(async (payload: EditSignupPhoneRequestDto) => {
    setLoading(true);
    setError(null);
    try {
      return await editSignupPhoneApi(payload);
    } catch (e: any) {
      const msg = extractApiMessage(e, "Failed to update phone number.");
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  return { editPhone, loading, error };
}