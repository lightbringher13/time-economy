// src/features/auth/register/queries/signupSession.queries.ts
import { useQuery } from "@tanstack/react-query";
import { getSignupStatusApi } from "../api/signupApi";
import { signupSessionKeys } from "./signupSession.keys";

export function useSignupStatusQuery(enabled = true) {
  return useQuery({
    queryKey: signupSessionKeys.status(),
    queryFn: getSignupStatusApi,
    enabled,
    staleTime: 0,
    refetchOnWindowFocus: false,
    retry: false,
  });
}