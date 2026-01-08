import { useCallback, useState } from "react";
import { extractApiMessage } from "./extractApiMessage";
import { updateSignupProfileApi } from "../api/signupApi";
import type { UpdateSignupProfileRequestDto } from "../api/signupApi.types";

export function useUpdateSignupProfile() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateProfile = useCallback(
    async (payload: UpdateSignupProfileRequestDto): Promise<void> => {
      setLoading(true);
      setError(null);

      try {
        await updateSignupProfileApi(payload);
      } catch (e: any) {
        const msg = extractApiMessage(e, "Failed to update profile.");
        setError(msg);
        throw e;
      } finally {
        setLoading(false);
      }
    },
    []
  );

  return { updateProfile, loading, error, setError };
}