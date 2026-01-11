import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { cancelSignupSessionApi } from "../api/signupApi";

export function useCancelSignupSessionMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: cancelSignupSessionApi,
    onSuccess: () => qc.invalidateQueries({ queryKey: signupSessionKeys.root }),
  });
}