// src/features/auth/register/queries/useSendSignupOtpMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { sendSignupOtpApi } from "../api/signupApi";
import { signupSessionKeys } from "./signupSession.keys";

export function useSendSignupOtpMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: sendSignupOtpApi,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}