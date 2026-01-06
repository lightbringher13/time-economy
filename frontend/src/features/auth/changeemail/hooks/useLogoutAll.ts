import { useState } from "react";
import { logoutAllApi } from "../../common/api/authApi";

export function useLogoutAll() {
  const [loading, setLoading] = useState(false);

  const logoutAll = async () => {
    setLoading(true);
    try {
      await logoutAllApi();
    } finally {
      setLoading(false);
    }
  };

  return { logoutAll, loading };
}