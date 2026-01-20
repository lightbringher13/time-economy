// src/features/auth/register/queries/useUpdateSignupProfileMutation.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { AxiosError } from "axios";

import { updateSignupProfileApi } from "../api/signupApi";
import type {
  UpdateSignupProfileRequestDto,
  UpdateSignupProfileResponseDto,
  SignupStatusResponseDto,
} from "../api/signupApi.types";
import { signupSessionKeys } from "./signupSession.keys";

type ApiErrorResponse = {
  code: string;
  message: string;
};

export function useUpdateSignupProfileMutation() {
  const qc = useQueryClient();

  return useMutation<
    UpdateSignupProfileResponseDto,          // ✅ only success DTO on 2xx
    AxiosError<ApiErrorResponse>,            // ✅ error DTO on non-2xx
    UpdateSignupProfileRequestDto
  >({
    mutationFn: updateSignupProfileApi,

    onSuccess: (res, vars) => {
      // ✅ optimistic cache update only on success
      qc.setQueryData<SignupStatusResponseDto>(signupSessionKeys.status(), (prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          name: vars.name,
          gender: vars.gender,
          birthDate: vars.birthDate,
          state: res.state,
        };
      });

      // ✅ background refetch (source of truth)
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },

    onError: () => {
      // keep status fresh (optional, but safe)
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}