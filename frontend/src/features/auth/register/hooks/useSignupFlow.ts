import { createContext, useContext } from "react";
import type { useSignupFlowImpl } from "./useSignupFlowImpl";

type SignupFlow = ReturnType<typeof useSignupFlowImpl>;

export const SignupFlowContext = createContext<SignupFlow | null>(null);

export function useSignupFlow() {
  const flow = useContext(SignupFlowContext);
  if (!flow) {
    throw new Error("useSignupFlow must be used within <SignupFlowProvider />");
  }
  return flow;
}
