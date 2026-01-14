// src/features/auth/register/queries/useCancelSignupSessionMutation.ts
import { useMutation } from "@tanstack/react-query";
import { cancelSignupSessionApi } from "../api/signupApi";

export function useCancelSignupSessionMutation() {
  return useMutation({
    mutationFn: cancelSignupSessionApi,
  });
}