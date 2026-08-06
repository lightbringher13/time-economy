import { useMemo, type ReactNode } from "react";
import { useSignupFlowImpl } from "./useSignupFlowImpl";
import { SignupFlowContext } from "./useSignupFlow";

export function SignupFlowProvider({ children }: { children: ReactNode }) {
  const flow = useSignupFlowImpl();

  // ✅ stable reference unless flow object identity changes
  const value = useMemo(() => flow, [flow]);

  return (
    <SignupFlowContext.Provider value={value}>
      {children}
    </SignupFlowContext.Provider>
  );
}
