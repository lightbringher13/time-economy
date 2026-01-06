// hooks/useStartSecondFactor.ts
import { useState } from "react";
import { startSecondFactorApi } from "../api/changeEmailApi";

export function useStartSecondFactor() {
  const [loading, setLoading] = useState(false);

  const start = async (payload: { requestId: number }) => {
    setLoading(true);
    try {
      return await startSecondFactorApi(payload);
    } finally {
      setLoading(false);
    }
  };

  return { loading, start };
}