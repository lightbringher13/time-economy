// src/features/auth/register/components/forms/SignupPhoneStep.tsx
import type { UseFormReturn } from "react-hook-form";
import type { SignupPhoneFormValues } from "../../forms/schemas/signupPhone.schema";

type Props = {
  title?: string;
  subtitle?: string;

  phoneVerified?: boolean;
  maskedPhone?: string | null;
  error?: string | null;

  phoneForm: UseFormReturn<SignupPhoneFormValues>;

  // behavior
  onSend: () => void | Promise<void>;
  onCancel?: () => void;

  // state
  sending?: boolean;
  cancelling?: boolean;

  // input control
  phoneDisabled?: boolean;
  sendLabel?: string;
  cancelLabel?: string;
};

export function SignupPhoneStep({
  title = "Create your account",
  subtitle = "Step 2 — Verify phone",

  phoneVerified = false,
  maskedPhone,
  error,

  phoneForm,

  onSend,
  onCancel,

  sending = false,
  cancelling = false,

  phoneDisabled = false,
  sendLabel = "Send code",
  cancelLabel = "Cancel",
}: Props) {
  const {
    register,
    formState: { errors },
  } = phoneForm;

  const busy = sending || cancelling;
  const inputDisabled = busy || phoneDisabled;

  return (
    <div>
      <h2>{title}</h2>
      <h3 style={{ marginTop: 8 }}>{subtitle}</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>{error}</div>
      )}

      <div style={{ marginTop: 16 }}>
        <label htmlFor="phoneNumber" style={{ display: "block", marginBottom: 4 }}>
          Phone number
        </label>

        <input
          id="phoneNumber"
          type="tel"
          autoComplete="tel"
          disabled={inputDisabled}
          {...register("phoneNumber")}
          style={{ width: "100%", padding: 8 }}
        />

        {errors.phoneNumber?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.phoneNumber.message}
          </div>
        )}

        {phoneVerified && (
          <div style={{ marginTop: 8, color: "#1a7f37", fontSize: 13 }}>
            Phone verified ✅ {maskedPhone ? `(${maskedPhone})` : ""}
          </div>
        )}
      </div>

      <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
        <button
          type="button"
          onClick={onSend}
          disabled={busy}
          style={{ padding: "8px 14px" }}
        >
          {sending ? "Sending..." : sendLabel}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            style={{ padding: "8px 14px" }}
          >
            {cancelling ? "Cancelling..." : cancelLabel}
          </button>
        )}
      </div>
    </div>
  );
}