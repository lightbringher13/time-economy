// src/features/auth/register/queries/useRegisterMutation.ts
import { useMutation } from "@tanstack/react-query";
import { registerApi } from "../api/registerApi";

export function useRegisterMutation() {
  return useMutation({
    mutationFn: registerApi,
  });
}