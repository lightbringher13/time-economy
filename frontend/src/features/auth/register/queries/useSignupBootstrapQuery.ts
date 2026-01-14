// src/features/auth/register/queries/useSignupBootstrapQuery.ts
import { useQuery } from "@tanstack/react-query";
import { signupBootstrapApi } from "../api/signupApi";
import { signupSessionKeys } from "./signupSession.keys";

export function useSignupBootstrapQuery(enabled = true) {
  return useQuery({
    queryKey: signupSessionKeys.bootstrap(), // add this key (see below)
    queryFn: signupBootstrapApi,
    enabled,
    staleTime: Infinity,           // ✅ run once and keep result
    refetchOnWindowFocus: false,
    retry: false,
  });
}