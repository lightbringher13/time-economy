// src/features/auth/register/queries/useCancelSignupSessionMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { cancelSignupSessionApi } from "../api/signupApi";
import { signupSessionKeys } from "./signupSession.keys";

export function useCancelSignupSessionMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: cancelSignupSessionApi,
    onSuccess: () => {
      qc.removeQueries({ queryKey: signupSessionKeys.status(), exact: true });
      // optional: qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}