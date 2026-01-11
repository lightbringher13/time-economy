import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { sendSignupOtpApi } from "../api/signupApi";

export function useSendSignupOtpMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: sendSignupOtpApi,
    onSuccess: () => qc.invalidateQueries({ queryKey: signupSessionKeys.root }),
  });
}