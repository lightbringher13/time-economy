// hooks/useEmailChangeStatus.ts
import { useCallback, useEffect, useRef, useState } from "react";
import { getActiveEmailChangeApi, getEmailChangeStatusApi } from "../api/changeEmailApi";
import type {
  EmailChangeStatus,
  GetEmailChangeStatusResponseDto,
  SecondFactorType,
} from "../api/changeEmailApi.types";

export type { EmailChangeStatus };

export function useEmailChangeStatus() {
  const [requestId, setRequestId] = useState<number | null>(null);
  const [status, setStatus] = useState<EmailChangeStatus | null>(null);
  const [secondFactorType, setSecondFactorType] = useState<SecondFactorType | null>(null);
  const [maskedNewEmail, setMaskedNewEmail] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const pollRef = useRef<number | null>(null);

  const apply = useCallback((dto: GetEmailChangeStatusResponseDto) => {
    setRequestId(dto.requestId ?? null);
    setStatus(dto.status ?? null);
    setSecondFactorType(dto.secondFactorType ?? null);
    setMaskedNewEmail(dto.maskedNewEmail ?? null);
    setExpiresAt(dto.expiresAt ?? null);
  }, []);

  const stopPolling = useCallback(() => {
    if (pollRef.current) window.clearInterval(pollRef.current);
    pollRef.current = null;
  }, []);

  const refresh = useCallback(async (id?: number) => {
    const dto = id ? await getEmailChangeStatusApi(id) : await getActiveEmailChangeApi();
    apply(dto);
    return dto;
  }, [apply]);

  // initial: fetch active
  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        const dto = await getActiveEmailChangeApi();
        if (!mounted) return;
        apply(dto);
      } catch {
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
  }, [apply, stopPolling]);

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
  }, [requestId, status, refresh, stopPolling]);

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
