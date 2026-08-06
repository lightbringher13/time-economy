import { createContext, useContext } from "react";
import type { SignupOtpUi } from "./useSignupOtpUi";

export const SignupOtpUiContext = createContext<SignupOtpUi | null>(null);

export function useSignupOtpUiContext() {
  const context = useContext(SignupOtpUiContext);
  if (!context) {
    throw new Error("useSignupOtpUiContext must be used within SignupOtpUiProvider");
  }
  return context;
}
