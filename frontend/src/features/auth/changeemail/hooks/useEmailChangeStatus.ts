// hooks/useEmailChangeStatus.ts
import { useEffect, useRef, useState } from "react";
import { getActiveEmailChangeApi, getEmailChangeStatusApi } from "../api/changeEmailApi";

export type EmailChangeStatus =
  | "PENDING"
  | "NEW_EMAIL_VERIFIED"
  | "SECOND_FACTOR_PENDING"
  | "READY_TO_COMMIT"
  | "COMPLETED"
  | "CANCELED"
  | "EXPIRED";

export function useEmailChangeStatus() {
  const [requestId, setRequestId] = useState<number | null>(null);
  const [status, setStatus] = useState<EmailChangeStatus | null>(null);
  const [secondFactorType, setSecondFactorType] = useState<"PHONE" | "OLD_EMAIL" | null>(null);
  const [maskedNewEmail, setMaskedNewEmail] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const pollRef = useRef<number | null>(null);

  const apply = (dto: any) => {
    setRequestId(dto.requestId ?? null);
    setStatus(dto.status ?? null);
    setSecondFactorType(dto.secondFactorType ?? null);
    setMaskedNewEmail(dto.maskedNewEmail ?? null);
    setExpiresAt(dto.expiresAt ?? null);
  };

  const stopPolling = () => {
    if (pollRef.current) window.clearInterval(pollRef.current);
    pollRef.current = null;
  };

  const refresh = async (id?: number) => {
    const dto = id ? await getEmailChangeStatusApi(id) : await getActiveEmailChangeApi();
    apply(dto);
    return dto;
  };

  // initial: fetch active
  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        const dto = await getActiveEmailChangeApi();
        if (!mounted) return;
        apply(dto);
      } catch (e: any) {
        // /active can return 204 — your apiClient should handle it.
        // If your apiClient throws on 204, you'll adjust later.
      } finally {
        if (mounted) setLoading(false);
      }
    })();

    return () => {
      mounted = false;
      stopPolling();
    };
  }, []);

  // polling only while flow is in-flight
  useEffect(() => {
    stopPolling();

    if (!requestId) return;
    if (!status) return;

    const inflight = status !== "COMPLETED" && status !== "CANCELED" && status !== "EXPIRED";
    if (!inflight) return;

    pollRef.current = window.setInterval(() => {
      refresh(requestId).catch(() => {});
    }, 1500);

    return stopPolling;
  }, [requestId, status]);

  return {
    loading,
    requestId,
    status,
    secondFactorType,
    maskedNewEmail,
    expiresAt,
    refresh,
    setRequestId, // wrapper can set after /request
    stopPolling,
  };
}