import { useMemo, type ReactNode } from "react";
import { useSignupFlow } from "./useSignupFlow";
import { useSignupOtpUi } from "./useSignupOtpUi";
import { SignupOtpUiContext } from "./useSignupOtpUiContext";

export function SignupOtpUiProvider({ children }: { children: ReactNode }) {
  const flow = useSignupFlow();

  // flow.status is your SignupStatusResponseDto | null
  const otpUi = useSignupOtpUi(flow.status);

  // small memo to keep stable reference
  const value = useMemo(() => otpUi, [otpUi]);

  return <SignupOtpUiContext.Provider value={value}>{children}</SignupOtpUiContext.Provider>;
}
