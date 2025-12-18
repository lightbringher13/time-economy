import { useCallback, useState } from "react";
import type {
  CreateLinkRequest,
  CreateLinkResponse,
  VerifyLinkRequest,
  VerifyLinkResponse,
} from "../types/verificationTypes";
import { createLinkApi, verifyLinkApi } from "../api/verificationApi";

export function useLinkVerification() {
  const [creating, setCreating] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [lastCreate, setLastCreate] = useState<CreateLinkResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const createLink = useCallback(async (req: CreateLinkRequest) => {
    setCreating(true);
    setError(null);
    try {
      const res = await createLinkApi(req);
      setLastCreate(res);
      return res;
    } catch (e: any) {
      setError(e?.response?.data?.message ?? e?.message ?? "Failed to create link");
      throw e;
    } finally {
      setCreating(false);
    }
  }, []);

  const verifyLink = useCallback(async (req: VerifyLinkRequest) => {
    setVerifying(true);
    setError(null);
    try {
      const res: VerifyLinkResponse = await verifyLinkApi(req);
      return res;
    } catch (e: any) {
      setError(e?.response?.data?.message ?? e?.message ?? "Failed to verify link");
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
    createLink,
    verifyLink,
  };
}