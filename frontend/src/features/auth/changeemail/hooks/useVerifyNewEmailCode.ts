// hooks/useVerifyNewEmailCode.ts
import { useState } from "react";
import { verifyNewEmailCodeApi } from "../api/changeEmailApi";

export function useVerifyNewEmailCode() {
  const [loading, setLoading] = useState(false);

  const verify = async (payload: { requestId: number; code: string }) => {
    setLoading(true);
    try {
      return await verifyNewEmailCodeApi(payload);
    } finally {
      setLoading(false);
    }
  };

  return { loading, verify };
}