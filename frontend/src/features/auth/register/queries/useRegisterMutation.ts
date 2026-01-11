import { useMutation, useQueryClient } from "@tanstack/react-query";
import { registerApi } from "../api/registerApi";

import { signupSessionKeys } from "./signupSession.keys";

export function useRegisterMutation() {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: registerApi,

    onSuccess: () => {
      // after register, backend clears signup cookie.
      // status query should now become "EXPIRED/NOT_FOUND" or "COMPLETED" depending on your BE,
      // so force refetch.
      qc.invalidateQueries({ queryKey: signupSessionKeys.root });
    },
  });
}