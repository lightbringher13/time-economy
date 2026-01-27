// src/features/auth/register/queries/useSendSignupOtpMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { sendSignupOtpApi } from "../api/signupApi";
import type {
  SendSignupOtpRequestDto,
  SendSignupOtpResponseDto,
  SignupStatusResponseDto,
} from "../api/signupApi.types";
import { signupSessionKeys } from "./signupSession.keys";

function makeEmptyStatus(): SignupStatusResponseDto {
  return {
    exists: false,
    email: null,
    emailVerified: false,
    emailOtpPending: false,
    emailOtpExpiresAt: null,     // ✅

    phoneNumber: null,
    phoneVerified: false,
    phoneOtpPending: false,
    phoneOtpExpiresAt: null,     // ✅

    name: null,
    gender: null,
    birthDate: null,
    state: "DRAFT",
  };
}

export function useSendSignupOtpMutation() {
  const qc = useQueryClient();

  return useMutation<SendSignupOtpResponseDto, unknown, SendSignupOtpRequestDto>({
    mutationFn: sendSignupOtpApi,

    onSuccess: (res, vars) => {
      const accepted =
        res.outcome === "SENT" ||
        res.outcome === "ALREADY_VERIFIED" ||
        res.outcome === "THROTTLED";

      if (accepted) {
        qc.setQueryData<SignupStatusResponseDto>(signupSessionKeys.status(), (prev) => {
          const base = prev ?? makeEmptyStatus();

          return {
            ...base,
            exists: true,
            state: res.state,

            emailVerified: res.emailVerified,
            phoneVerified: res.phoneVerified,
            emailOtpPending: res.emailOtpPending,
            phoneOtpPending: res.phoneOtpPending,

            // ✅ persist expiresAt into status cache (so UI countdown survives refetch)
            emailOtpExpiresAt: res.emailOtpExpiresAt ?? base.emailOtpExpiresAt,
            phoneOtpExpiresAt: res.phoneOtpExpiresAt ?? base.phoneOtpExpiresAt,

            // destination reflection (optional)
            email: vars.target === "EMAIL" ? vars.destination : base.email,
            phoneNumber: vars.target === "PHONE" ? vars.destination : base.phoneNumber,
          };
        });
      }

      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },

    onError: () => {
      // keep status accurate even on error
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}