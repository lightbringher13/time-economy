import { useState } from "react";
import { commitEmailChangeApi } from "../api/changeEmailApi";

export function useCommitEmailChange() {
  const [loading, setLoading] = useState(false);

  const commit = async (payload: { requestId: number }) => {
    setLoading(true);
    try {
      return await commitEmailChangeApi(payload);
    } finally {
      setLoading(false);
    }
  };

  return { loading, commit };
}