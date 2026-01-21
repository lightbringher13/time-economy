// src/features/auth/register/queries/useRegisterMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { registerApi } from "../api/registerApi";
import type { RegisterRequestDto, RegisterResponseDto } from "../api/registerApi.types";
import { signupSessionKeys } from "./signupSession.keys";

export function useRegisterMutation() {
  const qc = useQueryClient();

  return useMutation<RegisterResponseDto, unknown, RegisterRequestDto>({
    mutationFn: registerApi,

    // ✅ only side-effects that are always safe on success
    onSuccess: () => {
      // cookie cleared on success -> signup session is gone
      qc.removeQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}