// src/features/auth/register/hooks/useSignupFlow.ts
import { useCallback, useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";

import type { SignupSessionState, UpdateSignupProfileRequestDto } from "@/features/auth/register/api/signupApi.types"
import type { RegisterRequest } from "../api/registerApi.types";

import { signupSessionKeys } from "../queries/signupSession.keys";
import { extractApiMessage } from "../queries/extractApiMessage";

// queries / mutations
import { useSignupStatusQuery } from "../queries/useSignupStatusQuery";
import { useSendSignupOtpMutation } from "../queries/useSendSignupOtpMutation";
import { useVerifySignupOtpMutation } from "../queries/useVerifySignupOtpMutation";
import { useUpdateSignupProfileMutation } from "../queries/useUpdateSignupProfileMutation";
import { useCancelSignupSessionMutation } from "../queries/useCancelSignupSessionMutation";
import { useRegisterMutation } from "../queries/useRegisterMutation";

function firstErrorMessage(errors: Array<unknown>, fallback: string) {
  for (const e of errors) {
    if (e) return extractApiMessage(e, fallback);
  }
  return null;
}

export function useSignupFlowImpl() {
  const qc = useQueryClient();

  // ✅ BE is safe without cookie: { exists:false, state:"DRAFT" }
  const statusQuery = useSignupStatusQuery(true);

  // mutations
  const sendOtpMu = useSendSignupOtpMutation();
  const verifyOtpMu = useVerifySignupOtpMutation();
  const updateProfileMu = useUpdateSignupProfileMutation();
  const cancelMu = useCancelSignupSessionMutation();
  const registerMu = useRegisterMutation();

  const status = statusQuery.data ?? null;

  const view = useMemo(() => {
    const s = status;
    return {
      exists: Boolean(s?.exists),

      email: s?.email ?? null,
      emailVerified: Boolean(s?.emailVerified),
      emailOtpPending: Boolean((s as any)?.emailOtpPending), // if you added it to status dto

      phoneNumber: s?.phoneNumber ?? null,
      phoneVerified: Boolean(s?.phoneVerified),
      phoneOtpPending: Boolean((s as any)?.phoneOtpPending), // if you added it to status dto

      name: s?.name ?? null,
      gender: s?.gender ?? null,
      birthDate: s?.birthDate ?? null,

      state: (s?.state ?? null) as SignupSessionState | null,
    };
  }, [status]);

  const state = view.state;

  const loading = {
    page: statusQuery.isLoading,
    syncing: statusQuery.isFetching && !statusQuery.isLoading,

    sendOtp: sendOtpMu.isPending,
    verifyOtp: verifyOtpMu.isPending,
    updateProfile: updateProfileMu.isPending,
    cancel: cancelMu.isPending,
    register: registerMu.isPending,

    action:
      sendOtpMu.isPending ||
      verifyOtpMu.isPending ||
      updateProfileMu.isPending ||
      cancelMu.isPending ||
      registerMu.isPending,
  };

  const error = useMemo(
    () =>
      firstErrorMessage(
        [statusQuery.error, sendOtpMu.error, verifyOtpMu.error, updateProfileMu.error, cancelMu.error, registerMu.error],
        "Request failed."
      ),
    [statusQuery.error, sendOtpMu.error, verifyOtpMu.error, updateProfileMu.error, cancelMu.error, registerMu.error]
  );

  const refresh = useCallback(async () => {
    return await statusQuery.refetch();
  }, [statusQuery]);

  // optional helper (safe even if your mutation hooks already invalidate)
  const invalidateStatus = useCallback(() => {
    qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
  }, [qc]);

  // ✅ send otp now includes destination (this is your “edit + send”)
  const sendEmailOtp = useCallback(
    async (email: string) => {
      const dto = await sendOtpMu.mutateAsync({ target: "EMAIL", destination: email });
      invalidateStatus();
      return dto;
    },
    [sendOtpMu, invalidateStatus]
  );

  const sendPhoneOtp = useCallback(
    async (phoneNumber: string) => {
      const dto = await sendOtpMu.mutateAsync({ target: "PHONE", destination: phoneNumber });
      invalidateStatus();
      return dto;
    },
    [sendOtpMu, invalidateStatus]
  );

  // ✅ verify uses cookie only (no sessionId in payload)
  const verifyEmailOtp = useCallback(
    async (code: string) => {
      const dto = await verifyOtpMu.mutateAsync({ target: "EMAIL", code });
      invalidateStatus();
      return dto;
    },
    [verifyOtpMu, invalidateStatus]
  );

  const verifyPhoneOtp = useCallback(
    async (code: string) => {
      const dto = await verifyOtpMu.mutateAsync({ target: "PHONE", code });
      invalidateStatus();
      return dto;
    },
    [verifyOtpMu, invalidateStatus]
  );

  const updateProfile = useCallback(
    async (payload: UpdateSignupProfileRequestDto) => {
      const dto = await updateProfileMu.mutateAsync(payload);
      invalidateStatus();
      return dto;
    },
    [updateProfileMu, invalidateStatus]
  );

  /**
   * ✅ cancel: DO NOT navigate here (hook can’t).
   * Pages should do: navigate(LOGIN) first, then await flow.cancel()
   */
  const cancel = useCallback(async () => {
    const dto = await cancelMu.mutateAsync();

    // drop cached signup data so layout/status don't keep old errors around
    qc.removeQueries({ queryKey: signupSessionKeys.status(), exact: true });
    qc.removeQueries({ queryKey: signupSessionKeys.root });

    return dto;
  }, [cancelMu, qc]);

  /**
   * ✅ register: DO NOT clear cache here (it can race with navigation/layout).
   * Pages should:
   *   await flow.register(...)
   *   navigate("/signup/done", { replace:true })
   *   flow.clearSignupCache()  // optionally after navigation
   */
  const register = useCallback(async (payload: RegisterRequest) => {
    return await registerMu.mutateAsync(payload);
  }, [registerMu]);

  const clearSignupCache = useCallback(() => {
    qc.removeQueries({ queryKey: signupSessionKeys.status(), exact: true });
    qc.removeQueries({ queryKey: signupSessionKeys.root });
  }, [qc]);

  return {
    statusQuery,

    status,
    state,
    view,

    loading,
    error,

    refresh,

    // actions
    sendEmailOtp,
    sendPhoneOtp,
    verifyEmailOtp,
    verifyPhoneOtp,
    updateProfile,
    cancel,
    register,
    clearSignupCache,

    mutations: {
      sendOtpMu,
      verifyOtpMu,
      updateProfileMu,
      cancelMu,
      registerMu,
    },
  };
}