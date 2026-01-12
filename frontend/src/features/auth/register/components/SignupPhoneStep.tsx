// src/features/auth/register/components/SignupPhoneStep.tsx
import type { UseFormReturn } from "react-hook-form";

import type {
  SignupPhoneFormValues,
  SignupPhoneOtpFormValues,
} from "../forms/schemas/signupPhone.schema";

type PhoneUi = {
  title?: string; // default: "Create your account"
  subtitle?: string; // default: "Step 2 — Verify phone"

  // what to show
  showOtpBox?: boolean;
  showSend?: boolean;
  showEdit?: boolean;
  showBack?: boolean;
  showSkip?: boolean;
  showCancel?: boolean;

  // optional labels
  sendLabel?: string; // default: "Send SMS code" (parent can set "Resend code")
  editLabel?: string; // default: "Update phone"
  backLabel?: string; // default: "Back"
  skipLabel?: string; // default: "Continue"
  cancelLabel?: string; // default: "Cancel"
};

type PhoneLoading = {
  send?: boolean;
  verify?: boolean;
  edit?: boolean;
  cancel?: boolean;
};

type PhoneActions = {
  send?: () => void | Promise<void>;
  verify?: (code: string) => void | Promise<void>;
  edit?: (newPhone: string) => void | Promise<void>;
  back?: () => void | Promise<void>;
  skip?: () => void | Promise<void>;
  cancel?: () => void | Promise<void>;
};

interface Props {
  phoneVerified: boolean;
  maskedPhone?: string | null;
  error?: string | null;

  // forms
  phoneForm: UseFormReturn<SignupPhoneFormValues>;
  otpForm: UseFormReturn<SignupPhoneOtpFormValues>;

  // grouped props
  ui: PhoneUi;
  loading?: PhoneLoading;
  actions: PhoneActions;
}

export function SignupPhoneStep({
  phoneVerified,
  maskedPhone,
  error,
  phoneForm,
  otpForm,
  ui,
  loading,
  actions,
}: Props) {
  const {
    register: registerPhone,
    formState: { errors: phoneErrors },
    getValues: getPhoneValues,
  } = phoneForm;

  const {
    register: registerOtp,
    formState: { errors: otpErrors },
    handleSubmit: handleOtpSubmit,
    reset: resetOtp,
  } = otpForm;

  const isSending = Boolean(loading?.send);
  const isVerifying = Boolean(loading?.verify);
  const isEditing = Boolean(loading?.edit);
  const isCancelling = Boolean(loading?.cancel);

  const busy = isSending || isVerifying || isEditing || isCancelling;

  const title = ui.title ?? "Create your account";
  const subtitle = ui.subtitle ?? "Step 2 — Verify phone";

  const onClickSend = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    // if edit() exists, persist phone first
    if (actions.edit) {
      const phone = getPhoneValues("phoneNumber");
      await actions.edit(phone);
    }

    resetOtp({ code: "" });
    await actions.send?.(); // same endpoint for "send" and "resend"
  };

  const onSubmitVerify = handleOtpSubmit(async (values) => {
    await actions.verify?.(values.code);
  });

  const onClickEditPhone = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = getPhoneValues("phoneNumber");
    await actions.edit?.(phone);
    resetOtp({ code: "" });
  };

  return (
    <div>
      <h2>{title}</h2>
      <h3 style={{ marginTop: 8 }}>{subtitle}</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>
          {error}
        </div>
      )}

      {/* Phone input */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="phoneNumber" style={{ display: "block", marginBottom: 4 }}>
          Phone number
        </label>

        <input
          id="phoneNumber"
          type="tel"
          autoComplete="tel"
          disabled={busy || phoneVerified}
          {...registerPhone("phoneNumber")}
          style={{ width: "100%", padding: 8 }}
        />

        {phoneErrors.phoneNumber?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {phoneErrors.phoneNumber.message}
          </div>
        )}

        {phoneVerified && (
          <div style={{ marginTop: 8, color: "#1a7f37", fontSize: 13 }}>
            Phone verified ✅ {maskedPhone ? `(${maskedPhone})` : ""}
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
            {isSending ? "Sending..." : ui.sendLabel ?? "Send SMS code"}
          </button>
        )}

        {ui.showEdit && (
          <button
            type="button"
            onClick={onClickEditPhone}
            disabled={busy || !actions.edit}
            style={{ padding: "8px 14px" }}
          >
            {isEditing ? "Updating..." : ui.editLabel ?? "Update phone"}
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
            Enter the 6-digit code you received via SMS.
          </div>

          <label htmlFor="phoneOtp" style={{ display: "block", marginBottom: 4 }}>
            Verification code
          </label>

          <input
            id="phoneOtp"
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