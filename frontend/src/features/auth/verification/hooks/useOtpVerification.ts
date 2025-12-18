import { useCallback, useState } from "react";
import type {
  CreateOtpRequest,
  CreateOtpResponse,
  VerifyOtpRequest,
  VerifyOtpResponse,
} from "../types/verificationTypes";
import { createOtpApi, verifyOtpApi } from "../api/verificationApi";

export function useOtpVerification() {
  const [creating, setCreating] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [lastCreate, setLastCreate] = useState<CreateOtpResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const createOtp = useCallback(async (req: CreateOtpRequest) => {
    setCreating(true);
    setError(null);
    try {
      const res = await createOtpApi(req);
      setLastCreate(res);
      return res;
    } catch (e: any) {
      setError(e?.response?.data?.message ?? e?.message ?? "Failed to create OTP");
      throw e;
    } finally {
      setCreating(false);
    }
  }, []);

  const verifyOtp = useCallback(async (req: VerifyOtpRequest) => {
    setVerifying(true);
    setError(null);
    try {
      const res: VerifyOtpResponse = await verifyOtpApi(req);
      return res;
    } catch (e: any) {
      setError(e?.response?.data?.message ?? e?.message ?? "Failed to verify OTP");
      throw e;
    } finally {
      setVerifying(false);
    }
  }, []);

  return {
    creating,
    verifying,
    lastCreate,
    error,
    createOtp,
    verifyOtp,
  };
}