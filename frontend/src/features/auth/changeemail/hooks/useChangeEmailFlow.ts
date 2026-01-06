// hooks/useChangeEmailFlow.ts
import { useMemo, useState } from "react";
import { useEmailChangeStatus } from "./useEmailChangeStatus";
import { useRequestEmailChange } from "./useRequestEmailChange";
import { useVerifyNewEmailCode } from "./useVerifyNewEmailCode";
import { useStartSecondFactor } from "./useStartSecondFactor";
import { useVerifySecondFactor } from "./useVerifySecondFactor";
import { useCommitEmailChange } from "./useCommitEmailChange";
import { useLogoutAll } from "./useLogoutAll";

import { useAuthStore } from "@/store/useAuthStore"; // zustand, example
// import { useNavigate } from "react-router-dom"; // example

export type UiStep = "REQUEST" | "VERIFY_NEW" | "SECOND_FACTOR" | "DONE";

export function useChangeEmailFlow() {
  const status = useEmailChangeStatus();

  const req = useRequestEmailChange();
  const verifyNew = useVerifyNewEmailCode();
  const start2fa = useStartSecondFactor();
  const verify2fa = useVerifySecondFactor();
  const commit = useCommitEmailChange();
  const logout = useLogoutAll();

  const authStore = useAuthStore();
  // const navigate = useNavigate();

  const [error, setError] = useState<string | null>(null);

  const uiStep: UiStep = useMemo(() => {
    if (status.loading) return "REQUEST";

    switch (status.status) {
      case null:
        return "REQUEST";
      case "PENDING":
        return "VERIFY_NEW";
      case "NEW_EMAIL_VERIFIED":
        // we may auto-call start-second-factor, or show a button
        return "VERIFY_NEW";
      case "SECOND_FACTOR_PENDING":
        return "SECOND_FACTOR";
      case "READY_TO_COMMIT":
        return "SECOND_FACTOR";
      case "COMPLETED":
        return "DONE";
      case "CANCELED":
      case "EXPIRED":
      default:
        return "REQUEST";
    }
  }, [status.loading, status.status]);

  // -------- actions --------
  const submitRequest = async (payload: { currentPassword: string; newEmail: string }) => {
    setError(null);
    const res = await req.request(payload);
    status.setRequestId(res.requestId);
    await status.refresh(res.requestId); // sync status from server
    return res;
  };

  const submitVerifyNew = async (code: string) => {
    if (!status.requestId) throw new Error("No requestId");
    setError(null);

    const res = await verifyNew.verify({ requestId: status.requestId, code });

    // after verifying new email, trigger 2fa sending
    await start2fa.start({ requestId: status.requestId });
    await status.refresh(status.requestId);

    return res;
  };

  const submitSecondFactor = async (code: string) => {
    if (!status.requestId) throw new Error("No requestId");
    setError(null);

    const res = await verify2fa.verify({ requestId: status.requestId, code });

    await commit.commit({ requestId: status.requestId });

    await status.refresh(status.requestId);

    return res;
  };

  const finishAfterDone = async () => {
    // 1) revoke all sessions (server deletes refresh cookie)
    await logout.logoutAll();

    // 2) clear FE access token (important)
    authStore.logout(); // or authStore.reset()

    // 3) redirect
    // navigate("/login", { replace: true });
  };

  const loading =
    status.loading ||
    req.loading ||
    verifyNew.loading ||
    start2fa.loading ||
    verify2fa.loading ||
    commit.loading ||
    logout.loading;

  return {
    // derived UI
    uiStep,
    loading,
    error,

    // status data
    requestId: status.requestId,
    status: status.status,
    secondFactorType: status.secondFactorType,
    maskedNewEmail: status.maskedNewEmail,
    expiresAt: status.expiresAt,

    // actions
    submitRequest,
    submitVerifyNew,
    submitSecondFactor,
    finishAfterDone,

    // utility
    refresh: status.refresh,
  };
}