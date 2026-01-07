import { useState } from "react";
import { resendNewEmailOtpApi } from "../api/changeEmailApi";

export function useResendNewEmailOtp() {
  const [loading, setLoading] = useState(false);

  const resend = async (requestId: number) => {
    setLoading(true);
    try {
      await resendNewEmailOtpApi(requestId);
    } finally {
      setLoading(false);
    }
  };

  return { resend, loading };
}