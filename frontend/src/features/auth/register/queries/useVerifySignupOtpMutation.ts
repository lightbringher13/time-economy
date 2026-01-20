// src/features/auth/register/queries/useVerifySignupOtpMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { AxiosError } from "axios";

import { verifySignupOtpApi } from "../api/signupApi";
import type {
  VerifySignupOtpRequestDto,
  VerifySignupOtpResponseDto,
  SignupStatusResponseDto,
} from "../api/signupApi.types";
import { signupSessionKeys } from "./signupSession.keys";
import type { ApiErrorResponse } from "@/shared/api/apiError"; // { code, message }

export function useVerifySignupOtpMutation() {
  const qc = useQueryClient();

  return useMutation<VerifySignupOtpResponseDto, unknown, VerifySignupOtpRequestDto>({
    mutationFn: verifySignupOtpApi,

    onSuccess: (res) => {
      // ✅ only VERIFIED should reach here (200)
      qc.setQueryData<SignupStatusResponseDto>(signupSessionKeys.status(), (prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          exists: true,
          state: res.state,
          emailVerified: res.emailVerified,
          phoneVerified: res.phoneVerified,
          emailOtpPending: res.emailOtpPending,
          phoneOtpPending: res.phoneOtpPending,
        };
      });

      // ✅ still refetch in background (source of truth)
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },

    onError: (err) => {
      // keep status accurate (session/state might have changed)
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });

      // No toasts here. Page will read ApiErrorResponse and decide:
      // - inline for WRONG_CODE
      // - banner for NO_SESSION / NO_PENDING_OTP / INVALID_INPUT / network, etc.
      const ax = err as AxiosError<ApiErrorResponse>;
      void ax; // just to make it obvious the page owns presentation
    },
  });
}