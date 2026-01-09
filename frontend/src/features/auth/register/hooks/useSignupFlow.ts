// src/features/auth/register/hooks/useSignupFlow.ts
import { useCallback, useMemo, useState } from "react";

import { registerApi } from "../api/registerApi";

import type { RegisterRequest, RegisterResponse } from "../api/registerApi.types";

import {
  signupBootstrapApi,
  getSignupStatusApi,
  sendSignupOtpApi,
  resendSignupOtpApi,
  verifySignupOtpApi,
  editSignupEmailApi,
  editSignupPhoneApi,
  cancelSignupSessionApi,
  updateSignupProfileApi,
  // TODO: add this if you have it:
  // updateSignupProfileApi,
} from "../api/signupApi";

import type {
  SignupSessionState,
  SignupBootstrapResponseDto,
  SignupStatusResponseDto,
  SendSignupOtpResponseDto,
  ResendSignupOtpResponseDto,
  VerifySignupOtpResponseDto,
  EditSignupEmailRequestDto,
  EditSignupPhoneRequestDto,
  SendSignupOtpRequestDto,
  ResendSignupOtpRequestDto,
  VerifySignupOtpRequestDto,
  CancelSignupSessionResponseDto,
  UpdateSignupProfileRequestDto
} from "../api/signupApi.types";

import { extractApiMessage } from "./extractApiMessage";

export type SignupUiStep =
  | "EMAIL"          // enter email + send/resend
  | "EMAIL_VERIFY"   // enter email OTP
  | "PHONE"          // enter phone + send/resend
  | "PHONE_VERIFY"   // enter SMS OTP
  | "PROFILE"
  | "REVIEW"        // name/gender/birthDate
  | "DONE"
  | "CANCELED"
  | "EXPIRED";

function mapStateToUiStep(state?: SignupSessionState | null): SignupUiStep {
  switch (state) {
    case "DRAFT":
      return "EMAIL";
    case "EMAIL_OTP_SENT":
      return "EMAIL_VERIFY";
    case "EMAIL_VERIFIED":
      return "PHONE";
    case "PHONE_OTP_SENT":
      return "PHONE_VERIFY";
    case "PHONE_VERIFIED":
    case "PROFILE_PENDING":
      return "PROFILE";
    case "PROFILE_READY":
      return "REVIEW";
    case "COMPLETED":
      return "DONE";
    case "CANCELED":
      return "CANCELED";
    case "EXPIRED":
    default:
      return "EXPIRED";
  }
}

type FlowStatus = SignupStatusResponseDto | SignupBootstrapResponseDto | null;

/**
 * One hook for the whole signup flow:
 * - server-driven status (state machine)
 * - page actions (send/resend/verify/edit/cancel)
 */
export function useSignupFlow() {
  const [status, setStatus] = useState<FlowStatus>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const state = (status as any)?.state as SignupSessionState | undefined;

  const uiStep = useMemo(() => mapStateToUiStep(state), [state]);

  // ----- common helper -----
  const run = useCallback(async <T,>(fn: () => Promise<T>, fallbackMsg: string) => {
    setLoading(true);
    setError(null);
    try {
      return await fn();
    } catch (e: any) {
      setError(extractApiMessage(e, fallbackMsg));
      throw e;
    } finally {
      setLoading(false);
    }
  }, []);

  // ----- bootstrap / refresh -----
  const bootstrap = useCallback(async () => {
    const dto = await run(() => signupBootstrapApi(), "Failed to bootstrap signup session.");
    setStatus(dto);
    return dto;
  }, [run]);

  const refresh = useCallback(async () => {
    const dto = await run(() => getSignupStatusApi(), "Failed to load signup status.");
    setStatus(dto);
    return dto;
  }, [run]);

  // ----- OTP actions -----
  const sendOtp = useCallback(
    async (payload: SendSignupOtpRequestDto): Promise<SendSignupOtpResponseDto> => {
      const dto = await run(
        () => sendSignupOtpApi(payload),
        "Failed to send verification code."
      );

      // If your BE also updates session.state (recommended), refresh is optional.
      // But safe: refresh after mutations to keep FE consistent.
      await refresh().catch(() => {});
      return dto;
    },
    [run, refresh]
  );

  const resendOtp = useCallback(
    async (payload: ResendSignupOtpRequestDto): Promise<ResendSignupOtpResponseDto> => {
      const dto = await run(
        () => resendSignupOtpApi(payload),
        "Failed to resend verification code."
      );
      await refresh().catch(() => {});
      return dto;
    },
    [run, refresh]
  );

  const verifyOtp = useCallback(
    async (payload: VerifySignupOtpRequestDto): Promise<VerifySignupOtpResponseDto> => {
      const dto = await run(
        () => verifySignupOtpApi(payload),
        "Invalid or expired verification code."
      );
      await refresh().catch(() => {});
      return dto;
    },
    [run, refresh]
  );

  const updateProfile = useCallback(
    async (payload: UpdateSignupProfileRequestDto): Promise<void> => {
      await run(() => updateSignupProfileApi(payload), "Failed to update profile.");
      await refresh().catch(() => {});
    },
    [run, refresh]
  );

  const register = useCallback(
    async (payload: RegisterRequest): Promise<RegisterResponse> => {
      const dto = await run(() => registerApi(payload), "Failed to create account.");

      // ✅ since cookie is cleared, don't refresh signup status anymore
      // Instead, force the local flow state to DONE.
      setStatus((prev: any) => ({
        ...(prev ?? {}),
        state: "COMPLETED",
      }));

      return dto;
    },
    [run]
  );

  // convenience wrappers (cleaner in components)
  const sendEmailOtp = useCallback(
    () => sendOtp({ target: "EMAIL" } as any),
    [sendOtp]
  );
  const resendEmailOtp = useCallback(
    () => resendOtp({ target: "EMAIL" } as any),
    [resendOtp]
  );
  const verifyEmailOtp = useCallback(
    (code: string) => verifyOtp({ target: "EMAIL", code } as any),
    [verifyOtp]
  );

  const sendPhoneOtp = useCallback(
    () => sendOtp({ target: "PHONE" } as any),
    [sendOtp]
  );
  const resendPhoneOtp = useCallback(
    () => resendOtp({ target: "PHONE" } as any),
    [resendOtp]
  );
  const verifyPhoneOtp = useCallback(
    (code: string) => verifyOtp({ target: "PHONE", code } as any),
    [verifyOtp]
  );

  // ----- edit actions -----
  const editEmail = useCallback(
    async (payload: EditSignupEmailRequestDto) => {
      const dto = await run(() => editSignupEmailApi(payload), "Failed to update email.");
      // edit endpoints usually return updated status; if yours does, you can setStatus(dto)
      await refresh().catch(() => {});
      return dto;
    },
    [run, refresh]
  );

  const editPhone = useCallback(
    async (payload: EditSignupPhoneRequestDto) => {
      const dto = await run(() => editSignupPhoneApi(payload), "Failed to update phone number.");
      await refresh().catch(() => {});
      return dto;
    },
    [run, refresh]
  );

  // ----- cancel -----
  const cancel = useCallback(async (): Promise<CancelSignupSessionResponseDto> => {
    const dto = await run(() => cancelSignupSessionApi(), "Failed to cancel signup session.");
    await refresh().catch(() => {});
    return dto;
  }, [run, refresh]);

  // ----- derived data for UI -----
  const view = useMemo(() => {
    const s: any = status ?? {};
    return {
      exists: Boolean(s.exists ?? s.hasSession ?? true),
      email: s.email ?? null,
      emailVerified: Boolean(s.emailVerified),
      phoneNumber: s.phoneNumber ?? null,
      phoneVerified: Boolean(s.phoneVerified),
      name: s.name ?? null,
      gender: s.gender ?? null,
      birthDate: s.birthDate ?? null,
      state: (s.state ?? null) as SignupSessionState | null,
    };
  }, [status]);

  return {
    // state
    status,
    state: view.state,
    uiStep,
    view,

    // flags
    loading,
    error,
    setError,

    // lifecycle
    bootstrap,
    refresh,

    // actions (generic)
    sendOtp,
    resendOtp,
    verifyOtp,
    updateProfile,
    register,

    // actions (convenience)
    sendEmailOtp,
    resendEmailOtp,
    verifyEmailOtp,
    sendPhoneOtp,
    resendPhoneOtp,
    verifyPhoneOtp,

    // edit/cancel
    editEmail,
    editPhone,
    cancel,
  };
}