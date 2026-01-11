import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { editSignupPhoneApi } from "../api/signupApi";

export function useEditSignupPhoneMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: editSignupPhoneApi,
    onSuccess: () => qc.invalidateQueries({ queryKey: signupSessionKeys.root }),
  });
}