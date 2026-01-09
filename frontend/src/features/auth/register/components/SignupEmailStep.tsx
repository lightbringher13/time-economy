// src/features/auth/register/components/SignupEmailStep.tsx
import type { UseFormReturn } from "react-hook-form";

import type { SignupEmailFormValues } from "../forms/schemas/signupEmail.schema";
import type { SignupEmailOtpFormValues } from "../forms/schemas/signupEmail.schema";
import type {SignupSessionState} from "@/features/auth/register/api/signupApi.types"

interface Props {
  state: SignupSessionState | null;
  emailVerified: boolean;
  maskedEmail?: string | null; // optional: if your BE returns masked
  loading: boolean;
  error?: string | null;

  // forms
  emailForm: UseFormReturn<SignupEmailFormValues>;
  otpForm: UseFormReturn<SignupEmailOtpFormValues>;

  // handlers
  onSendOtp: () => void | Promise<void>;
  onResendOtp: () => void | Promise<void>;
  onVerifyOtp: (code: string) => void | Promise<void>;

  onEditEmail?: (newEmail: string) => void | Promise<void>;
  onCancel?: () => void | Promise<void>;
}

export function SignupEmailStep({
  state,
  emailVerified,
  maskedEmail,
  loading,
  error,
  emailForm,
  otpForm,
  onSendOtp,
  onResendOtp,
  onVerifyOtp,
  onEditEmail,
  onCancel,
}: Props) {
  const {
    register: registerEmail,
    formState: { errors: emailErrors },
    getValues: getEmailValues,
  } = emailForm;

  const {
    register: registerOtp,
    formState: { errors: otpErrors },
    handleSubmit: handleOtpSubmit,
    reset: resetOtp,
  } = otpForm;

  const showOtpBox =
    state === "EMAIL_OTP_SENT" && !emailVerified;

  const showSendButton =
    state === "DRAFT" && !emailVerified;

  const showResendButton =
    state === "EMAIL_OTP_SENT" && !emailVerified;

  const showEditButton =
    Boolean(onEditEmail) &&
    (state === "EMAIL_OTP_SENT" || state === "EMAIL_VERIFIED");

  const onClickSend = async () => {
    // validate email form first
    const ok = await emailForm.trigger("email");
    if (!ok) return;

    // optionally update email on server before sending OTP
    // (if your BE requires editEmail before send)
    if (onEditEmail) {
      const email = getEmailValues("email");
      await onEditEmail(email);
    }

    resetOtp({ code: "" });
    await onSendOtp();
  };

  const onClickResend = async () => {
    resetOtp({ code: "" });
    await onResendOtp();
  };

  const onSubmitVerify = handleOtpSubmit(async (values) => {
    await onVerifyOtp(values.code);
  });

  const onClickEditEmail = async () => {
    if (!onEditEmail) return;

    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = getEmailValues("email");
    await onEditEmail(email);

    // after edit, OTP is invalid; clear OTP input
    resetOtp({ code: "" });
  };

  return (
    <div>
      <h2>Create your account</h2>
      <h3 style={{ marginTop: 8 }}>Step 1 — Verify email</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>
          {error}
        </div>
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
          disabled={loading || emailVerified}
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
        {showSendButton && (
          <button type="button" onClick={onClickSend} disabled={loading} style={{ padding: "8px 14px" }}>
            {loading ? "Sending..." : "Send code"}
          </button>
        )}

        {showResendButton && (
          <button type="button" onClick={onClickResend} disabled={loading} style={{ padding: "8px 14px" }}>
            {loading ? "Resending..." : "Resend code"}
          </button>
        )}

        {showEditButton && (
          <button type="button" onClick={onClickEditEmail} disabled={loading} style={{ padding: "8px 14px" }}>
            Update email
          </button>
        )}

        {onCancel && (
          <button type="button" onClick={onCancel} disabled={loading} style={{ padding: "8px 14px" }}>
            Cancel
          </button>
        )}
      </div>

      {/* OTP verify box */}
      {showOtpBox && (
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
            disabled={loading}
            {...registerOtp("code")}
            style={{ width: "100%", padding: 8 }}
          />

          {otpErrors.code?.message && (
            <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
              {otpErrors.code.message}
            </div>
          )}

          <button type="submit" disabled={loading} style={{ marginTop: 12, padding: "8px 14px" }}>
            {loading ? "Verifying..." : "Verify"}
          </button>
        </form>
      )}
    </div>
  );
}