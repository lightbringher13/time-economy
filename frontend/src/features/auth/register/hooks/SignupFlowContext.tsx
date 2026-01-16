// src/features/auth/register/hooks/SignupFlowContext.tsx
import React, { createContext, useContext, useMemo } from "react";
import { useSignupFlowImpl } from "./useSignupFlowImpl";

type Flow = ReturnType<typeof useSignupFlowImpl>;

const SignupFlowContext = createContext<Flow | null>(null);

export function SignupFlowProvider({ children }: { children: React.ReactNode }) {
  const flow = useSignupFlowImpl();

  // ✅ stable reference unless flow object identity changes
  const value = useMemo(() => flow, [flow]);

  return (
    <SignupFlowContext.Provider value={value}>
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