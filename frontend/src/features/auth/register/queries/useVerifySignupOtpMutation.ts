import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { verifySignupOtpApi } from "../api/signupApi";

export function useVerifySignupOtpMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: verifySignupOtpApi,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: signupSessionKeys.status(), exact: true });
    },
  });
}