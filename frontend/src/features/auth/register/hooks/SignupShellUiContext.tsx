// src/features/auth/register/hooks/SignupShellUiContext.tsx
import React, { createContext, useContext, useMemo, useState, useCallback, useRef } from "react";
import { ConfirmModal } from "../components/modal/ConfirmModal";

type CancelReason = "user" | "timeout" | "nav";

type CancelModalState = {
  open: boolean;
  reason?: CancelReason;
  title?: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
};

type ConfirmModalState = {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
};

type OpenConfirmOptions = Omit<ConfirmModalState, "open"> & {
  onConfirm: () => void | Promise<void>;
  onCancel?: () => void;
};

type OpenCancelOptions = {
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
  // generic confirm (e.g., edit email/phone, leave page, etc.)
  openConfirmModal: (opts: OpenConfirmOptions) => void;
  closeConfirmModal: () => void;

  // cancel signup flow confirm (common copy defaults, but overridable)
  openCancelModal: (opts: OpenCancelOptions) => void;
  closeCancelModal: () => void;

  // optional helper
  closeAllModals: () => void;
};

const SignupShellUiContext = createContext<SignupShellUi | null>(null);

export function SignupShellUiProvider({ children }: { children: React.ReactNode }) {
  // ---- Cancel modal ----
  const [cancelModal, setCancelModal] = useState<CancelModalState>({ open: false });
  const cancelBusyRef = useRef(false);
  const [cancelBusy, setCancelBusy] = useState(false);
  const cancelConfirmRef = useRef<OpenCancelOptions["onConfirm"] | null>(null);
  const cancelCancelRef = useRef<OpenCancelOptions["onCancel"] | null>(null);

  const closeCancelModal = useCallback(() => {
    setCancelModal({ open: false });
    cancelConfirmRef.current = null;
    cancelCancelRef.current = null;
    cancelBusyRef.current = false;
    setCancelBusy(false);
  }, []);

  const openCancelModal = useCallback((opts: OpenCancelOptions) => {
    cancelConfirmRef.current = opts.onConfirm;
    cancelCancelRef.current = opts.onCancel ?? null;

    setCancelModal({
      open: true,
      reason: opts.reason,
      title: opts.title ?? "Cancel signup?",
      description:
        opts.description ??
        "Your signup progress will be discarded and you’ll return to the login page.",
      confirmLabel: opts.confirmLabel ?? "Cancel signup",
      cancelLabel: opts.cancelLabel ?? "Keep signing up",
      destructive: opts.destructive ?? true,
    });

    cancelBusyRef.current = false;
    setCancelBusy(false);
  }, []);

  const onCancelConfirm = useCallback(async () => {
    if (cancelBusyRef.current) return;
    const fn = cancelConfirmRef.current;
    if (!fn) return;

    try {
      cancelBusyRef.current = true;
      setCancelBusy(true);
      await fn();
      closeCancelModal();
    } catch {
      // keep modal open; caller can surface an error toast elsewhere if desired
      cancelBusyRef.current = false;
      setCancelBusy(false);
    }
  }, [closeCancelModal]);

  const onCancelDismiss = useCallback(() => {
    cancelCancelRef.current?.();
    closeCancelModal();
  }, [closeCancelModal]);

  // ---- Generic confirm modal ----
  const [confirmModal, setConfirmModal] = useState<ConfirmModalState | null>(null);
  const confirmBusyRef = useRef(false);
  const [confirmBusy, setConfirmBusy] = useState(false);
  const confirmConfirmRef = useRef<OpenConfirmOptions["onConfirm"] | null>(null);
  const confirmCancelRef = useRef<OpenConfirmOptions["onCancel"] | null>(null);

  const closeConfirmModal = useCallback(() => {
    setConfirmModal(null);
    confirmConfirmRef.current = null;
    confirmCancelRef.current = null;
    confirmBusyRef.current = false;
    setConfirmBusy(false);
  }, []);

  const openConfirmModal = useCallback((opts: OpenConfirmOptions) => {
    confirmConfirmRef.current = opts.onConfirm;
    confirmCancelRef.current = opts.onCancel ?? null;

    setConfirmModal({
      open: true,
      title: opts.title,
      description: opts.description,
      confirmLabel: opts.confirmLabel ?? "Continue",
      cancelLabel: opts.cancelLabel ?? "Cancel",
      destructive: opts.destructive ?? false,
    });

    confirmBusyRef.current = false;
    setConfirmBusy(false);
  }, []);

  const onConfirmConfirm = useCallback(async () => {
    if (confirmBusyRef.current) return;
    const fn = confirmConfirmRef.current;
    if (!fn) return;

    try {
      confirmBusyRef.current = true;
      setConfirmBusy(true);
      await fn();
      closeConfirmModal();
    } catch {
      confirmBusyRef.current = false;
      setConfirmBusy(false);
    }
  }, [closeConfirmModal]);

  const onConfirmDismiss = useCallback(() => {
    confirmCancelRef.current?.();
    closeConfirmModal();
  }, [closeConfirmModal]);

  const closeAllModals = useCallback(() => {
    closeConfirmModal();
    closeCancelModal();
  }, [closeConfirmModal, closeCancelModal]);

  const value = useMemo(
    () => ({
      openConfirmModal,
      closeConfirmModal,
      openCancelModal,
      closeCancelModal,
      closeAllModals,
    }),
    [openConfirmModal, closeConfirmModal, openCancelModal, closeCancelModal, closeAllModals]
  );

  return (
    <SignupShellUiContext.Provider value={value}>
      {children}

      {/* Cancel modal (signup-wide) */}
      <ConfirmModal
        open={cancelModal.open}
        title={cancelModal.title ?? "Cancel signup?"}
        description={cancelModal.description}
        confirmLabel={cancelModal.confirmLabel ?? "Cancel signup"}
        cancelLabel={cancelModal.cancelLabel ?? "Keep signing up"}
        destructive={cancelModal.destructive ?? true}
        busy={cancelBusy}
        onConfirm={onCancelConfirm}
        onCancel={onCancelDismiss}
      />

      {/* Generic confirm modal */}
      <ConfirmModal
        open={Boolean(confirmModal?.open)}
        title={confirmModal?.title ?? ""}
        description={confirmModal?.description}
        confirmLabel={confirmModal?.confirmLabel ?? "Continue"}
        cancelLabel={confirmModal?.cancelLabel ?? "Cancel"}
        destructive={confirmModal?.destructive ?? false}
        busy={confirmBusy}
        onConfirm={onConfirmConfirm}
        onCancel={onConfirmDismiss}
      />
    </SignupShellUiContext.Provider>
  );
}

export function useSignupShellUi() {
  const ctx = useContext(SignupShellUiContext);
  if (!ctx) throw new Error("useSignupShellUi must be used within <SignupShellUiProvider />");
  return ctx;
}