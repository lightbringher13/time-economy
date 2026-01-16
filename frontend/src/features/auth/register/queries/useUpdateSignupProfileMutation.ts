// src/features/auth/register/queries/useUpdateSignupProfileMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateSignupProfileApi } from "../api/signupApi";
import { signupSessionKeys } from "./signupSession.keys";

export function useUpdateSignupProfileMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: updateSignupProfileApi,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}