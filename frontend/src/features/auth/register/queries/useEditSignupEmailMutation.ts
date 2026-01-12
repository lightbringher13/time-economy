import { useMutation, useQueryClient } from "@tanstack/react-query";
import { signupSessionKeys } from "./signupSession.keys";

import { editSignupEmailApi } from "../api/signupApi";

export function useEditSignupEmailMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: editSignupEmailApi,
    onSuccess: () => qc.invalidateQueries({ queryKey: signupSessionKeys.root }),
  });
}