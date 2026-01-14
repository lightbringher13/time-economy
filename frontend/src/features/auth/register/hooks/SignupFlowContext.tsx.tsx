import React, { createContext, useContext } from "react";
import { useSignupFlowImpl } from "./useSignupFlow";

type Flow = ReturnType<typeof useSignupFlowImpl>;

const SignupFlowContext = createContext<Flow | null>(null);

export function SignupFlowProvider({ children }: { children: React.ReactNode }) {
  const flow = useSignupFlowImpl(); // ✅ single instance created here
  return (
    <SignupFlowContext.Provider value={flow}>
      {children}
    </SignupFlowContext.Provider>
  );
}

export function useSignupFlow() {
  const flow = useContext(SignupFlowContext);
  if (!flow) {
    throw new Error("useSignupFlow must be used within <SignupFlowProvider />");
  }
  return flow;
}