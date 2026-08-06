// src/features/auth/register/hooks/SignupShellUiContext.tsx
import {
  useMemo,
  useState,
  useCallback,
  useRef,
  type ReactNode,
} from "react";
import { ConfirmModal } from "../components/modal/ConfirmModal";
import {
  SignupShellUiContext,
  type CancelModalState,
  type ConfirmModalState,
  type OpenCancelOptions,
  type OpenConfirmOptions,
  type SignupBanner,
} from "./useSignupShellUi";

export function SignupShellUiProvider({ children }: { children: ReactNode }) {
  // ✅ NEW: banner state
  const [banner, setBanner] = useState<SignupBanner | null>(null);

  const showBanner = useCallback((b: SignupBanner) => setBanner(b), []);
  const clearBanner = useCallback(() => setBanner(null), []);

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

      // ✅ banner
      banner,
      showBanner,
      clearBanner,
    }),
    [
      openConfirmModal,
      closeConfirmModal,
      openCancelModal,
      closeCancelModal,
      closeAllModals,
      banner,
      showBanner,
      clearBanner,
    ]
  );

  return (
    <SignupShellUiContext.Provider value={value}>
      {/* ✅ Banner shown once for whole signup */}
      {banner && (
        <div style={{ maxWidth: 460, margin: "12px auto 0", padding: "0 16px" }}>
          <div
            role="alert"
            style={{
              borderRadius: 8,
              padding: 12,
              border:
                banner.kind === "error"
                  ? "1px solid #f5c2c7"
                  : banner.kind === "success"
                  ? "1px solid #badbcc"
                  : "1px solid #b6d4fe",
              background:
                banner.kind === "error"
                  ? "#f8d7da"
                  : banner.kind === "success"
                  ? "#d1e7dd"
                  : "#cfe2ff",
              color:
                banner.kind === "error"
                  ? "#842029"
                  : banner.kind === "success"
                  ? "#0f5132"
                  : "#084298",
              display: "flex",
              alignItems: "flex-start",
              gap: 10,
            }}
          >
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 700 }}>
                {banner.kind === "error"
                  ? "Something went wrong"
                  : banner.kind === "success"
                  ? "Success"
                  : "Notice"}
              </div>
              <div style={{ marginTop: 4, fontSize: 13 }}>{banner.message}</div>
              {banner.code ? (
                <div style={{ marginTop: 6, fontSize: 11, opacity: 0.8 }}>
                  {banner.code}
                </div>
              ) : null}
            </div>

            <button
              type="button"
              onClick={clearBanner}
              style={{
                border: "none",
                background: "transparent",
                cursor: "pointer",
                fontSize: 14,
                lineHeight: 1,
                padding: 4,
              }}
              aria-label="Dismiss"
            >
              ✕
            </button>
          </div>
        </div>
      )}

      {children}

      {/* Cancel modal */}
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
