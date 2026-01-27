// src/features/auth/register/hooks/SignupOtpUiContext.tsx
import React, { createContext, useContext, useMemo } from "react";
import { useSignupFlow } from "./SignupFlowContext";
import { useSignupOtpUi, type SignupOtpUi } from "./useSignupOtpUi";

type Ctx = SignupOtpUi;

const SignupOtpUiContext = createContext<Ctx | null>(null);

export function SignupOtpUiProvider({ children }: { children: React.ReactNode }) {
  const flow = useSignupFlow();

  // flow.status is your SignupStatusResponseDto | null
  const otpUi = useSignupOtpUi(flow.status);

  // small memo to keep stable reference
  const value = useMemo(() => otpUi, [otpUi]);

  return <SignupOtpUiContext.Provider value={value}>{children}</SignupOtpUiContext.Provider>;
}

export function useSignupOtpUiContext() {
  const ctx = useContext(SignupOtpUiContext);
  if (!ctx) throw new Error("useSignupOtpUiContext must be used within SignupOtpUiProvider");
  return ctx;
}