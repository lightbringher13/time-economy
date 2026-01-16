// src/features/auth/register/components/SignupEmailStep.tsx
import type { UseFormReturn } from "react-hook-form";
import type {
  SignupEmailFormValues,
  SignupEmailOtpFormValues,
} from "../forms/schemas/signupEmail.schema";

type EmailUi = {
  title?: string;
  subtitle?: string;

  showOtpBox?: boolean;
  showSend?: boolean;
  showEdit?: boolean;
  showBack?: boolean;
  showSkip?: boolean;
  showCancel?: boolean;

  // ✅ page decides input lock
  emailDisabled?: boolean;

  sendLabel?: string;
  editLabel?: string;
  backLabel?: string;
  skipLabel?: string;
  cancelLabel?: string;
};

type EmailLoading = {
  send?: boolean;
  verify?: boolean;
  edit?: boolean;
  cancel?: boolean;
};

type EmailActions = {
  send?: () => void | Promise<void>;
  verify?: (code: string) => void | Promise<void>;
  edit?: () => void | Promise<void>;

  back?: () => void | Promise<void>;
  skip?: () => void | Promise<void>;
  cancel?: () => void | Promise<void>;
};

interface Props {
  emailVerified: boolean;
  maskedEmail?: string | null;
  error?: string | null;

  emailForm: UseFormReturn<SignupEmailFormValues>;
  otpForm: UseFormReturn<SignupEmailOtpFormValues>;

  ui: EmailUi;
  loading?: EmailLoading;
  actions: EmailActions;
}

export function SignupEmailStep({
  emailVerified,
  maskedEmail,
  error,
  emailForm,
  otpForm,
  ui,
  loading,
  actions,
}: Props) {
  const {
    register: registerEmail,
    formState: { errors: emailErrors },
  } = emailForm;

  const {
    register: registerOtp,
    formState: { errors: otpErrors },
    handleSubmit: handleOtpSubmit,
  } = otpForm;

  const isSending = Boolean(loading?.send);
  const isVerifying = Boolean(loading?.verify);
  const isEditing = Boolean(loading?.edit);
  const isCancelling = Boolean(loading?.cancel);

  const busy = isSending || isVerifying || isEditing || isCancelling;

  const title = ui.title ?? "Create your account";
  const subtitle = ui.subtitle ?? "Step 1 — Verify email";

  const onClickSend = async () => {
    await actions.send?.();
  };

  const onSubmitVerify = handleOtpSubmit(async (values) => {
    await actions.verify?.(values.code);
  });

  const onClickEditEmail = async () => {
    await actions.edit?.();
  };

  const emailDisabled = busy || Boolean(ui.emailDisabled);

  return (
    <div>
      <h2>{title}</h2>
      <h3 style={{ marginTop: 8 }}>{subtitle}</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>{error}</div>
      )}

      {/* Email input */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="email" style={{ display: "block", marginBottom: 4 }}>
          Email
        </label>

        <input
          id="email"
          type="email"
          autoComplete="email"
          disabled={emailDisabled}
          {...registerEmail("email")}
          style={{ width: "100%", padding: 8 }}
        />

        {emailErrors.email?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {emailErrors.email.message}
          </div>
        )}

        {emailVerified && (
          <div style={{ marginTop: 8, color: "#1a7f37", fontSize: 13 }}>
            Email verified ✅ {maskedEmail ? `(${maskedEmail})` : ""}
          </div>
        )}
      </div>

      {/* Actions */}
      <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
        {ui.showSend && (
          <button
            type="button"
            onClick={onClickSend}
            disabled={busy || !actions.send}
            style={{ padding: "8px 14px" }}
          >
            {isSending ? "Sending..." : ui.sendLabel ?? "Send code"}
          </button>
        )}

        {ui.showEdit && (
          <button
            type="button"
            onClick={onClickEditEmail}
            disabled={busy || !actions.edit}
            style={{ padding: "8px 14px" }}
          >
            {isEditing ? "Updating..." : ui.editLabel ?? "Edit"}
          </button>
        )}

        {ui.showBack && (
          <button
            type="button"
            onClick={actions.back}
            disabled={busy || !actions.back}
            style={{ padding: "8px 14px" }}
          >
            {ui.backLabel ?? "Back"}
          </button>
        )}

        {ui.showSkip && (
          <button
            type="button"
            onClick={actions.skip}
            disabled={busy || !actions.skip}
            style={{ padding: "8px 14px" }}
          >
            {ui.skipLabel ?? "Continue"}
          </button>
        )}

        {ui.showCancel && (
          <button
            type="button"
            onClick={actions.cancel}
            disabled={busy || !actions.cancel}
            style={{ padding: "8px 14px" }}
          >
            {isCancelling ? "Cancelling..." : ui.cancelLabel ?? "Cancel"}
          </button>
        )}
      </div>

      {/* OTP verify box */}
      {ui.showOtpBox && (
        <form onSubmit={onSubmitVerify} noValidate style={{ marginTop: 18 }}>
          <div style={{ marginBottom: 8, fontSize: 13, color: "#666" }}>
            Enter the 6-digit code sent to your email.
          </div>

          <label htmlFor="emailOtp" style={{ display: "block", marginBottom: 4 }}>
            Verification code
          </label>

          <input
            id="emailOtp"
            inputMode="numeric"
            autoComplete="one-time-code"
            disabled={busy}
            {...registerOtp("code")}
            style={{ width: "100%", padding: 8 }}
          />

          {otpErrors.code?.message && (
            <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
              {otpErrors.code.message}
            </div>
          )}

          <button
            type="submit"
            disabled={busy || !actions.verify}
            style={{ marginTop: 12, padding: "8px 14px" }}
          >
            {isVerifying ? "Verifying..." : "Verify"}
          </button>
        </form>
      )}
    </div>
  );
}