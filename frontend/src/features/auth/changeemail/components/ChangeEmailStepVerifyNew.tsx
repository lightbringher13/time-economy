// src/features/auth/change-email/components/ChangeEmailStepVerifyNew.tsx
import React from "react";
import type { UseFormReturn } from "react-hook-form";
import type { VerifyNewEmailCodeFormValues } from "../forms/schemas/verifyNewEmailCode.schema";

interface Props {
  form: UseFormReturn<VerifyNewEmailCodeFormValues>;
  onSubmit: React.FormEventHandler<HTMLFormElement>;
  loading: boolean;
  error?: string | null;
  maskedNewEmail?: string | null;

  // actions (optional)
  onResend?: () => void;
  onCancel?: () => void;
}

export function ChangeEmailStepVerifyNew({
  form,
  onSubmit,
  loading,
  error,
  maskedNewEmail,
  onResend,
  onCancel,
}: Props) {
  const {
    register,
    formState: { errors },
  } = form;

  return (
    <form onSubmit={onSubmit} noValidate>
      <h2>Verify your new email</h2>

      {maskedNewEmail && (
        <div style={{ marginBottom: 12, fontSize: 14 }}>
          Verification email: <strong>{maskedNewEmail}</strong>
        </div>
      )}

      {error && (
        <div style={{ marginBottom: 12, color: "red", fontSize: 14 }}>
          {error}
        </div>
      )}

      <div style={{ marginBottom: 16 }}>
        <label htmlFor="code" style={{ display: "block", marginBottom: 4 }}>
          Verification code (6 digits)
        </label>
        <input
          id="code"
          inputMode="numeric"
          autoComplete="one-time-code"
          disabled={loading}
          {...register("code")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.code?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.code.message}
          </div>
        )}
      </div>

      <div style={{ display: "flex", gap: 8 }}>
        <button type="submit" disabled={loading} style={{ padding: "8px 14px" }}>
          {loading ? "Verifying..." : "Verify"}
        </button>

        {onResend && (
          <button
            type="button"
            disabled={loading}
            onClick={onResend}
            style={{ padding: "8px 14px" }}
          >
            Resend code
          </button>
        )}

        {onCancel && (
          <button
            type="button"
            disabled={loading}
            onClick={onCancel}
            style={{ padding: "8px 14px" }}
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}