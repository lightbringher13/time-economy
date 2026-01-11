import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { updateSignupProfileApi } from "../api/signupApi";

export function useUpdateSignupProfileMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: updateSignupProfileApi,
    onSuccess: () => qc.invalidateQueries({ queryKey: signupSessionKeys.root }),
  });
}