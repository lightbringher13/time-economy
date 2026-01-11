import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { resendSignupOtpApi } from "../api/signupApi";

export function useResendSignupOtpMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: resendSignupOtpApi,
    onSuccess: () => qc.invalidateQueries({ queryKey: signupSessionKeys.root }),
  });
}