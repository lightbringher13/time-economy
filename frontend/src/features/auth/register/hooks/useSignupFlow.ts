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
import { useSignupStatusQuery } from "../queries/useSignupStatusQuery";
import { useSignupBootstrapMutation } from "../queries/useSignupBootstrapMutation";
import { useSendSignupOtpMutation } from "../queries/useSendSignupOtpMutation";
import { useResendSignupOtpMutation } from "../queries/useResendSignupOtpMutation";
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

export function useSignupFlow() {
  const qc = useQueryClient();

  // ✅ single source of truth (server)
  const statusQuery = useSignupStatusQuery(true);

  // mutations (ideally each one invalidates signupSessionKeys.status() in onSuccess)
  const bootstrapMu = useSignupBootstrapMutation();
  const sendOtpMu = useSendSignupOtpMutation();
  const resendOtpMu = useResendSignupOtpMutation();
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

  // global loading/error
  // ✅ in useSignupFlow.ts (or even in each page)
  const loading = {
    // Page-blocking (rare): first load / bootstrap
    page:
      statusQuery.isLoading || // first time load
      bootstrapMu.isPending,   // creating session/cookie

    // Background sync (don’t block UI): refetch after invalidation, window refocus, etc.
    syncing:
      statusQuery.isFetching && !statusQuery.isLoading,

    // Button-level actions
    sendOtp: sendOtpMu.isPending,
    resendOtp: resendOtpMu.isPending,
    verifyOtp: verifyOtpMu.isPending,
    editEmail: editEmailMu.isPending,
    editPhone: editPhoneMu.isPending,
    updateProfile: updateProfileMu.isPending,
    cancel: cancelMu.isPending,
    register: registerMu.isPending,

    // Useful “any action running?”
    action:
      sendOtpMu.isPending ||
      resendOtpMu.isPending ||
      verifyOtpMu.isPending ||
      editEmailMu.isPending ||
      editPhoneMu.isPending ||
      updateProfileMu.isPending ||
      cancelMu.isPending ||
      registerMu.isPending,
  };

  const error = useMemo(
    () =>
      firstErrorMessage(
        [
          statusQuery.error,
          bootstrapMu.error,
          sendOtpMu.error,
          resendOtpMu.error,
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
      statusQuery.error,
      bootstrapMu.error,
      sendOtpMu.error,
      resendOtpMu.error,
      verifyOtpMu.error,
      editEmailMu.error,
      editPhoneMu.error,
      updateProfileMu.error,
      cancelMu.error,
      registerMu.error,
    ]
  );

  // lifecycle
  const bootstrap = useCallback(async () => {
    const dto = await bootstrapMu.mutateAsync();
    // make sure status reflects new cookie/session
    await qc.invalidateQueries({ queryKey: signupSessionKeys.root });
    return dto;
  }, [bootstrapMu, qc]);

  const refresh = useCallback(async () => {
    return await statusQuery.refetch();
  }, [statusQuery]);

  // actions
  const sendEmailOtp = useCallback(async () => {
    return await sendOtpMu.mutateAsync({ target: "EMAIL" });
  }, [sendOtpMu]);

  const resendEmailOtp = useCallback(async () => {
    return await resendOtpMu.mutateAsync({ target: "EMAIL" });
  }, [resendOtpMu]);

  const verifyEmailOtp = useCallback(
    async (code: string) => {
      return await verifyOtpMu.mutateAsync({ target: "EMAIL", code });
    },
    [verifyOtpMu]
  );

  const sendPhoneOtp = useCallback(async () => {
    return await sendOtpMu.mutateAsync({ target: "PHONE" });
  }, [sendOtpMu]);

  const resendPhoneOtp = useCallback(async () => {
    return await resendOtpMu.mutateAsync({ target: "PHONE" });
  }, [resendOtpMu]);

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
    // session dead -> drop all signup cache
    qc.removeQueries({ queryKey: signupSessionKeys.root });
    return dto;
  }, [cancelMu, qc]);

  /**
   * ✅ register: NO local “force DONE”.
   * Your UI should navigate to DONE page on success using the mutation result.
   * (And since BE clears cookie, status may stop working after this.)
   */
  const register = useCallback(
    async (payload: RegisterRequest) => {
      const dto = await registerMu.mutateAsync(payload);

      // optional: clear signup cache to avoid refetch noise after cookie deletion
      qc.removeQueries({ queryKey: signupSessionKeys.root });

      return dto;
    },
    [registerMu, qc]
  );

  return {
    // server state
    status, // raw dto
    state,  // convenience
    view,

    // flags
    loading,
    error,

    // lifecycle
    bootstrap,
    refresh, // manual only (debug)

    // actions
    sendEmailOtp,
    resendEmailOtp,
    verifyEmailOtp,
    sendPhoneOtp,
    resendPhoneOtp,
    verifyPhoneOtp,
    editEmail,
    editPhone,
    updateProfile,
    cancel,
    register,

    // expose queries/mutations if you want fine-grained control at pages
    statusQuery,
    mutations: {
      bootstrapMu,
      sendOtpMu,
      resendOtpMu,
      verifyOtpMu,
      editEmailMu,
      editPhoneMu,
      updateProfileMu,
      cancelMu,
      registerMu,
    },
  };
}