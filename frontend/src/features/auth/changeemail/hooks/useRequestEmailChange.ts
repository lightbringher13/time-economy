// hooks/useRequestEmailChange.ts
import { useState } from "react";
import { requestEmailChangeApi } from "../api/changeEmailApi";

export function useRequestEmailChange() {
  const [loading, setLoading] = useState(false);

  const request = async (payload: { currentPassword: string; newEmail: string }) => {
    setLoading(true);
    try {
      return await requestEmailChangeApi(payload);
    } finally {
      setLoading(false);
    }
  };

  return { loading, request };
}