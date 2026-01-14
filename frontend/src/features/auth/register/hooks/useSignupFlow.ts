// src/features/auth/register/hooks/useSignupFlow.ts
import { useCallback, useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";

import type {
  SignupSessionState,
  UpdateSignupProfileRequestDto,
} from "../api/signupApi.types";
import type { RegisterRequest } from "../api/registerApi.types";

import { signupSessionKeys } from "../queries/signupSession.keys";
import { extractApiMessage } from "../queries/extractApiMessage";

// queries / mutations
import { useSignupBootstrapQuery } from "../queries/useSignupBootstrapQuery";
import { useSignupStatusQuery } from "../queries/useSignupStatusQuery";
import { useSendSignupOtpMutation } from "../queries/useSendSignupOtpMutation";
import { useVerifySignupOtpMutation } from "../queries/useVerifySignupOtpMutation";
import { useEditSignupEmailMutation } from "../queries/useEditSignupEmailMutation";
import { useEditSignupPhoneMutation } from "../queries/useEditSignupPhoneMutation";
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

  // ✅ 1) bootstrap runs as a query (cached, StrictMode-safe)
  const bootstrapQ = useSignupBootstrapQuery(true);

  // ✅ 2) status runs only after bootstrap succeeded (cookie/session ready)
  const statusQuery = useSignupStatusQuery(bootstrapQ.isSuccess);

  // mutations
  const sendOtpMu = useSendSignupOtpMutation();
  const verifyOtpMu = useVerifySignupOtpMutation();
  const editEmailMu = useEditSignupEmailMutation();
  const editPhoneMu = useEditSignupPhoneMutation();
  const updateProfileMu = useUpdateSignupProfileMutation();
  const cancelMu = useCancelSignupSessionMutation();
  const registerMu = useRegisterMutation();

  // expose raw data
  const status = statusQuery.data ?? null;
  const state = (status?.state ?? null) as SignupSessionState | null;

  const view = useMemo(() => {
    const s = status;
    return {
      email: s?.email ?? null,
      emailVerified: Boolean(s?.emailVerified),
      phoneNumber: s?.phoneNumber ?? null,
      phoneVerified: Boolean(s?.phoneVerified),
      name: s?.name ?? null,
      gender: s?.gender ?? null,
      birthDate: s?.birthDate ?? null,
      state: (s?.state ?? null) as SignupSessionState | null,
    };
  }, [status]);

  // global loading flags
  const loading = {
    // Page-blocking (rare): bootstrap + first status load
    page: bootstrapQ.isLoading || statusQuery.isLoading,

    // Background sync (don’t block UI)
    syncing: statusQuery.isFetching && !statusQuery.isLoading,

    // Button-level actions
    sendOtp: sendOtpMu.isPending,
    verifyOtp: verifyOtpMu.isPending,
    editEmail: editEmailMu.isPending,
    editPhone: editPhoneMu.isPending,
    updateProfile: updateProfileMu.isPending,
    cancel: cancelMu.isPending,
    register: registerMu.isPending,

    // Useful “any action running?”
    action:
      sendOtpMu.isPending ||
      verifyOtpMu.isPending ||
      editEmailMu.isPending ||
      editPhoneMu.isPending ||
      updateProfileMu.isPending ||
      cancelMu.isPending ||
      registerMu.isPending,
  };

  // global error message
  const error = useMemo(
    () =>
      firstErrorMessage(
        [
          bootstrapQ.error,
          statusQuery.error,
          sendOtpMu.error,
          verifyOtpMu.error,
          editEmailMu.error,
          editPhoneMu.error,
          updateProfileMu.error,
          cancelMu.error,
          registerMu.error,
        ],
        "Request failed."
      ),
    [
      bootstrapQ.error,
      statusQuery.error,
      sendOtpMu.error,
      verifyOtpMu.error,
      editEmailMu.error,
      editPhoneMu.error,
      updateProfileMu.error,
      cancelMu.error,
      registerMu.error,
    ]
  );

  // manual refresh (debug)
  const refresh = useCallback(async () => {
    return await statusQuery.refetch();
  }, [statusQuery]);

  // actions
  const sendEmailOtp = useCallback(async () => {
    return await sendOtpMu.mutateAsync({ target: "EMAIL" });
  }, [sendOtpMu]);

  const verifyEmailOtp = useCallback(
    async (code: string) => {
      return await verifyOtpMu.mutateAsync({ target: "EMAIL", code });
    },
    [verifyOtpMu]
  );

  const sendPhoneOtp = useCallback(async () => {
    return await sendOtpMu.mutateAsync({ target: "PHONE" });
  }, [sendOtpMu]);

  const verifyPhoneOtp = useCallback(
    async (code: string) => {
      return await verifyOtpMu.mutateAsync({ target: "PHONE", code });
    },
    [verifyOtpMu]
  );

  const editEmail = useCallback(
    async (newEmail: string) => {
      return await editEmailMu.mutateAsync({ newEmail });
    },
    [editEmailMu]
  );

  const editPhone = useCallback(
    async (newPhoneNumber: string) => {
      return await editPhoneMu.mutateAsync({ newPhoneNumber });
    },
    [editPhoneMu]
  );

  const updateProfile = useCallback(
    async (payload: UpdateSignupProfileRequestDto) => {
      return await updateProfileMu.mutateAsync(payload);
    },
    [updateProfileMu]
  );

  const cancel = useCallback(async () => {
    const dto = await cancelMu.mutateAsync();

    // ✅ clear cached status/bootstrap so old "cookie not found" doesn't stick
    qc.removeQueries({ queryKey: signupSessionKeys.status(), exact: true });
    qc.removeQueries({ queryKey: signupSessionKeys.bootstrap(), exact: true });
    qc.removeQueries({ queryKey: signupSessionKeys.root });

    return dto;
  }, [cancelMu, qc]);

  const register = useCallback(
    async (payload: RegisterRequest) => {
      const dto = await registerMu.mutateAsync(payload);
      return dto;
    },
    [registerMu, qc]
  );

  const clearSignupCache = useCallback(() => {
    qc.removeQueries({ queryKey: signupSessionKeys.status(), exact: true });
    qc.removeQueries({ queryKey: signupSessionKeys.bootstrap(), exact: true });
    qc.removeQueries({ queryKey: signupSessionKeys.root });
  }, [qc]);

  return {
    // ✅ queries (exposed at top-level)
    bootstrapQ,
    statusQuery,

    // server state
    status,
    state,
    view,

    // flags
    loading,
    error,

    // lifecycle
    refresh,

    // actions
    sendEmailOtp,
    verifyEmailOtp,
    sendPhoneOtp,
    verifyPhoneOtp,
    editEmail,
    editPhone,
    updateProfile,
    cancel,
    register,
    clearSignupCache,

    // expose mutations if you want fine-grained control in pages
    mutations: {
      sendOtpMu,
      verifyOtpMu,
      editEmailMu,
      editPhoneMu,
      updateProfileMu,
      cancelMu,
      registerMu,
    },
  };
}