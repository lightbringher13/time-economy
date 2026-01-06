// hooks/useVerifySecondFactor.ts
import { useState } from "react";
import { verifySecondFactorApi } from "../api/changeEmailApi";

export function useVerifySecondFactor() {
  const [loading, setLoading] = useState(false);

  const verify = async (payload: { requestId: number; code: string }) => {
    setLoading(true);
    try {
      return await verifySecondFactorApi(payload);
    } finally {
      setLoading(false);
    }
  };

  return { loading, verify };
}