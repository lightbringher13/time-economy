import { useState } from "react";
import { cancelEmailChangeApi } from "../api/changeEmailApi";

export function useCancelEmailChange() {
  const [loading, setLoading] = useState(false);

  const cancel = async (requestId: number) => {
    setLoading(true);
    try {
      await cancelEmailChangeApi(requestId);
    } finally {
      setLoading(false);
    }
  };

  return { cancel, loading };
}