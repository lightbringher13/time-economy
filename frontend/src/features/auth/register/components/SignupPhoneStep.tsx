// src/features/auth/register/components/SignupPhoneStep.tsx
import type { UseFormReturn } from "react-hook-form";

import type { SignupPhoneFormValues } from "../forms/schemas/signupPhone.schema";
import type { SignupPhoneOtpFormValues } from "../forms/schemas/signupPhone.schema";
import type {SignupSessionState} from "@/features/auth/register/api/signupApi.types"


interface Props {
  state: SignupSessionState | null;
  phoneVerified: boolean;
  maskedPhone?: string | null; // optional if BE returns masked
  loading: boolean;
  error?: string | null;

  // forms
  phoneForm: UseFormReturn<SignupPhoneFormValues>;
  otpForm: UseFormReturn<SignupPhoneOtpFormValues>;

  // handlers
  onSendOtp: () => void | Promise<void>;
  onResendOtp: () => void | Promise<void>;
  onVerifyOtp: (code: string) => void | Promise<void>;

  onEditPhone?: (newPhone: string) => void | Promise<void>;
  onBackToEmail?: () => void | Promise<void>; // optional (you can map to editEmail/cancel)
  onCancel?: () => void | Promise<void>;
}

export function SignupPhoneStep({
  state,
  phoneVerified,
  maskedPhone,
  loading,
  error,
  phoneForm,
  otpForm,
  onSendOtp,
  onResendOtp,
  onVerifyOtp,
  onEditPhone,
  onBackToEmail,
  onCancel,
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

  const canUsePhoneStep = 
  state === "EMAIL_VERIFIED" ||
  state === "PHONE_OTP_SENT" ||
  state === "PROFILE_PENDING" ||
  state === "COMPLETED";

  const showOtpBox = state === "PHONE_OTP_SENT" && !phoneVerified;

  const showSendButton = state === "EMAIL_VERIFIED" && !phoneVerified;
  const showResendButton = state === "PHONE_OTP_SENT" && !phoneVerified;

  const showEditButton =
    Boolean(onEditPhone) && (state === "PHONE_OTP_SENT" || state === "EMAIL_VERIFIED" || state === "PROFILE_PENDING");

  const onClickSend = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    if (onEditPhone) {
      const phone = getPhoneValues("phoneNumber");
      await onEditPhone(phone); // persist phone into session (optional but common)
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

  const onClickEditPhone = async () => {
    if (!onEditPhone) return;

    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = getPhoneValues("phoneNumber");
    await onEditPhone(phone);
    resetOtp({ code: "" });
  };

  if (!canUsePhoneStep) {
    return (
      <div>
        <h2>Verify phone</h2>
        <p style={{ marginTop: 8, color: "#666" }}>
          Please verify your email first.
        </p>

        {onBackToEmail && (
          <button type="button" onClick={onBackToEmail} style={{ marginTop: 12, padding: "8px 14px" }}>
            Back
          </button>
        )}
      </div>
    );
  }

  return (
    <div>
      <h2>Create your account</h2>
      <h3 style={{ marginTop: 8 }}>Step 2 — Verify phone</h3>

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
          disabled={loading || phoneVerified}
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
        {showSendButton && (
          <button type="button" onClick={onClickSend} disabled={loading} style={{ padding: "8px 14px" }}>
            {loading ? "Sending..." : "Send SMS code"}
          </button>
        )}

        {showResendButton && (
          <button type="button" onClick={onClickResend} disabled={loading} style={{ padding: "8px 14px" }}>
            {loading ? "Resending..." : "Resend code"}
          </button>
        )}

        {showEditButton && (
          <button type="button" onClick={onClickEditPhone} disabled={loading} style={{ padding: "8px 14px" }}>
            Update phone
          </button>
        )}

        {onBackToEmail && (
          <button type="button" onClick={onBackToEmail} disabled={loading} style={{ padding: "8px 14px" }}>
            Back
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
            Enter the 6-digit code you received via SMS.
          </div>

          <label htmlFor="phoneOtp" style={{ display: "block", marginBottom: 4 }}>
            Verification code
          </label>

          <input
            id="phoneOtp"
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