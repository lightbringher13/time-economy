import { createContext, useContext } from "react";

type CancelReason = "user" | "timeout" | "nav";

export type SignupBannerKind = "error" | "info" | "success";

export type SignupBanner = {
  kind: SignupBannerKind;
  message: string;
  code?: string;
};

export type CancelModalState = {
  open: boolean;
  reason?: CancelReason;
  title?: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
};

export type ConfirmModalState = {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
};

export type OpenConfirmOptions = Omit<ConfirmModalState, "open"> & {
  onConfirm: () => void | Promise<void>;
  onCancel?: () => void;
};

export type OpenCancelOptions = {
  reason?: CancelReason;
  title?: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  onConfirm: () => void | Promise<void>;
  onCancel?: () => void;
};

type SignupShellUi = {
  openConfirmModal: (options: OpenConfirmOptions) => void;
  closeConfirmModal: () => void;
  openCancelModal: (options: OpenCancelOptions) => void;
  closeCancelModal: () => void;
  closeAllModals: () => void;
  banner: SignupBanner | null;
  showBanner: (banner: SignupBanner) => void;
  clearBanner: () => void;
};

export const SignupShellUiContext = createContext<SignupShellUi | null>(null);

export function useSignupShellUi() {
  const context = useContext(SignupShellUiContext);
  if (!context) {
    throw new Error("useSignupShellUi must be used within <SignupShellUiProvider />");
  }
  return context;
}
