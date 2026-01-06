// src/features/auth/change-email/components/ChangeEmailStepRequest.tsx
import React from "react";
import type { UseFormReturn } from "react-hook-form";
import type { RequestEmailChangeFormValues } from "../forms/schemas/requestEmailChange.schema";

interface Props {
  form: UseFormReturn<RequestEmailChangeFormValues>;
  onSubmit: React.FormEventHandler<HTMLFormElement>;
  loading: boolean;
  error?: string | null;
  currentEmail?: string;
}

export function ChangeEmailStepRequest({
  form,
  onSubmit,
  loading,
  error,
  currentEmail,
}: Props) {
  const {
    register,
    formState: { errors },
  } = form;

  return (
    <form onSubmit={onSubmit} noValidate>
      <h2>Email change</h2>

      {currentEmail && (
        <div style={{ marginBottom: 12, fontSize: 14 }}>
          Current email: <strong>{currentEmail}</strong>
        </div>
      )}

      {error && (
        <div style={{ marginBottom: 12, color: "red", fontSize: 14 }}>
          {error}
        </div>
      )}

      <div style={{ marginBottom: 16 }}>
        <label htmlFor="currentPassword" style={{ display: "block", marginBottom: 4 }}>
          Current password
        </label>
        <input
          id="currentPassword"
          type="password"
          autoComplete="current-password"
          disabled={loading}
          {...register("currentPassword")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.currentPassword?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.currentPassword.message}
          </div>
        )}
      </div>

      <div style={{ marginBottom: 16 }}>
        <label htmlFor="newEmail" style={{ display: "block", marginBottom: 4 }}>
          New email
        </label>
        <input
          id="newEmail"
          type="email"
          autoComplete="email"
          disabled={loading}
          {...register("newEmail")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.newEmail?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.newEmail.message}
          </div>
        )}
      </div>

      <p style={{ fontSize: 12, color: "#666", marginBottom: 16 }}>
        We’ll confirm your current password first, then verify that you own the new email.
      </p>

      <button type="submit" disabled={loading} style={{ padding: "8px 14px" }}>
        {loading ? "Sending..." : "Send verification code"}
      </button>
    </form>
  );
}