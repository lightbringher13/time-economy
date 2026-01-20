// src/features/auth/register/components/forms/SignupVerifyOtpStep.tsx
import type { UseFormReturn } from "react-hook-form";

export type SignupOtpFormValues = { code: string };

type Props = {
  title?: string;
  subtitle?: string;

  destinationLabel?: string; // "Email" | "Phone"
  maskedDestination?: string | null;

  error?: string | null;

  form: UseFormReturn<SignupOtpFormValues>;

  onVerify: (code: string) => void | Promise<void>;
  onCancel?: () => void | Promise<void>;

  verifying?: boolean;
  cancelling?: boolean;

  verifyLabel?: string;
  cancelLabel?: string;
};

export function SignupVerifyOtpStep({
  title = "Verify code",
  subtitle = "Enter the 6-digit code we sent",

  destinationLabel = "Destination",
  maskedDestination = null,

  error,

  form,

  onVerify,
  onCancel,

  verifying = false,
  cancelling = false,

  verifyLabel = "Verify",
  cancelLabel = "Cancel",
}: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form;

  const busy = verifying || cancelling;

  const submit = handleSubmit(async (values) => {
    await onVerify(values.code);
  });

  return (
    <div>
      <h2>{title}</h2>
      <h3 style={{ marginTop: 8 }}>{subtitle}</h3>

      {maskedDestination ? (
        <div style={{ marginTop: 12, fontSize: 13, color: "#333" }}>
          <span style={{ color: "#666" }}>{destinationLabel}:</span>{" "}
          <span style={{ fontWeight: 600 }}>{maskedDestination}</span>
        </div>
      ) : null}

      {error ? (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>{error}</div>
      ) : null}

      <form onSubmit={submit} noValidate style={{ marginTop: 16 }}>
        <label htmlFor="otp" style={{ display: "block", marginBottom: 4 }}>
          Verification code
        </label>

        <input
          id="otp"
          inputMode="numeric"
          autoComplete="one-time-code"
          disabled={busy}
          {...register("code")}
          style={{ width: "100%", padding: 8 }}
        />

        {errors.code?.message ? (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {String(errors.code.message)}
          </div>
        ) : null}

        <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
          <button type="submit" disabled={busy} style={{ padding: "8px 14px" }}>
            {verifying ? "Verifying..." : verifyLabel}
          </button>

          {onCancel ? (
            <button
              type="button"
              onClick={onCancel}
              disabled={busy}
              style={{ padding: "8px 14px" }}
            >
              {cancelling ? "Cancelling..." : cancelLabel}
            </button>
          ) : null}
        </div>
      </form>
    </div>
  );
}