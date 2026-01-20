// src/features/auth/register/queries/useCancelSignupSessionMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { cancelSignupSessionApi } from "../api/signupApi";
import type { CancelSignupSessionResponseDto } from "../api/signupApi.types";
import { signupSessionKeys } from "./signupSession.keys";

export function useCancelSignupSessionMutation() {
  const qc = useQueryClient();

  const clearSignupStatus = () => {
    qc.removeQueries({ queryKey: signupSessionKeys.status(), exact: true });
  };

  return useMutation<CancelSignupSessionResponseDto, unknown, void>({
    mutationFn: cancelSignupSessionApi,

    // ✅ always stop signup UI regardless of outcome
    onSettled: () => {
      clearSignupStatus();
    },
  });
}