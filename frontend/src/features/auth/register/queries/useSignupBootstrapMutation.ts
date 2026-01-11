import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { signupBootstrapApi } from "../api/signupApi";

export function useSignupBootstrapMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: signupBootstrapApi,
    onSuccess: () => qc.invalidateQueries({ queryKey: signupSessionKeys.root }),
  });
}