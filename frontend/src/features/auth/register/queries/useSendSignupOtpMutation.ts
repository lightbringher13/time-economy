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
    phoneNumber: null,
    phoneVerified: false,
    name: null,
    gender: null,
    birthDate: null,
    state: "DRAFT",
    emailOtpPending: false,
    phoneOtpPending: false,
  };
}

export function useSendSignupOtpMutation() {
  const qc = useQueryClient();

  return useMutation<SendSignupOtpResponseDto, unknown, SendSignupOtpRequestDto>({
    mutationFn: sendSignupOtpApi,

    onSuccess: (res, vars) => {
      // ✅ optimistic cache update only when request accepted
      if (res.outcome === "SENT" || res.outcome === "ALREADY_VERIFIED") {
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
            email: vars.target === "EMAIL" ? vars.destination : base.email,
            phoneNumber: vars.target === "PHONE" ? vars.destination : base.phoneNumber,
          };
        });
      }

      // ✅ backend is source of truth
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },

    onError: () => {
      // keep status accurate even on error
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}