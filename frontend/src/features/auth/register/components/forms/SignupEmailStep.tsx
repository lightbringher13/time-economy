// src/features/auth/register/components/forms/SignupEmailStep.tsx
import type { UseFormReturn } from "react-hook-form";
import type { SignupEmailFormValues } from "../../forms/schemas/signupEmail.schema";

type Props = {
  title?: string;
  subtitle?: string;

  emailVerified?: boolean;
  maskedEmail?: string | null;
  error?: string | null;

  emailForm: UseFormReturn<SignupEmailFormValues>;

  // behavior
  onSend: () => void | Promise<void>;
  onCancel?: () => void;

  // state
  sending?: boolean;
  cancelling?: boolean;

  // input control
  emailDisabled?: boolean;
  sendLabel?: string;
  cancelLabel?: string;
};

export function SignupEmailStep({
  title = "Create your account",
  subtitle = "Step 1 — Verify email",

  emailVerified = false,
  maskedEmail,
  error,

  emailForm,

  onSend,
  onCancel,

  sending = false,
  cancelling = false,

  emailDisabled = false,
  sendLabel = "Send code",
  cancelLabel = "Cancel",
}: Props) {
  const {
    register,
    formState: { errors },
  } = emailForm;

  const busy = sending || cancelling;
  const inputDisabled = busy || emailDisabled;

  return (
    <div>
      <h2>{title}</h2>
      <h3 style={{ marginTop: 8 }}>{subtitle}</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>{error}</div>
      )}

      <div style={{ marginTop: 16 }}>
        <label htmlFor="email" style={{ display: "block", marginBottom: 4 }}>
          Email
        </label>

        <input
          id="email"
          type="email"
          autoComplete="email"
          disabled={inputDisabled}
          {...register("email")}
          style={{ width: "100%", padding: 8 }}
        />

        {errors.email?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.email.message}
          </div>
        )}

        {emailVerified && (
          <div style={{ marginTop: 8, color: "#1a7f37", fontSize: 13 }}>
            Email verified ✅ {maskedEmail ? `(${maskedEmail})` : ""}
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