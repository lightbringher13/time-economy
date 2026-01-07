// src/features/auth/change-email/components/ChangeEmailStepSecondFactor.tsx
import React from "react";
import type { UseFormReturn } from "react-hook-form";
import type { VerifySecondFactorFormValues } from "../forms/schemas/verifySecondFactor.schema";

type SecondFactorType = "PHONE" | "OLD_EMAIL";

interface Props {
  form: UseFormReturn<VerifySecondFactorFormValues>;
  onSubmit: React.FormEventHandler<HTMLFormElement>;
  loading: boolean;
  error?: string | null;
  secondFactorType?: SecondFactorType | null;

  // ✅ new
  onCancel?: () => void;

  // ✅ optional (only if you implement a resend-2fa endpoint)
  onResend?: () => void;
}

export function ChangeEmailStepSecondFactor({
  form,
  onSubmit,
  loading,
  error,
  secondFactorType,
  onCancel,
  onResend,
}: Props) {
  const {
    register,
    formState: { errors },
  } = form;

  const helperText =
    secondFactorType === "PHONE"
      ? "Enter the 6-digit code you received via SMS."
      : secondFactorType === "OLD_EMAIL"
      ? "Enter the 6-digit code sent to your old email address."
      : "Enter the 6-digit code.";

  const title =
    secondFactorType === "PHONE" ? "Two-factor verification (SMS)" : "Two-factor verification";

  return (
    <form onSubmit={onSubmit} noValidate>
      <h2>{title}</h2>

      <p style={{ fontSize: 13, color: "#666", marginBottom: 12 }}>{helperText}</p>

      {error && (
        <div style={{ marginBottom: 12, color: "red", fontSize: 14 }}>
          {error}
        </div>
      )}

      <div style={{ marginBottom: 16 }}>
        <label htmlFor="code" style={{ display: "block", marginBottom: 4 }}>
          Verification code
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
          {loading ? "Processing..." : "Confirm"}
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