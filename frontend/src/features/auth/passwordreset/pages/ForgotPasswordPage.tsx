// features/auth/pages/ForgotPasswordPage.tsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { ROUTES } from "@/routes/paths";
import { requestPasswordResetApi } from "../api/passwordResetApi";
import { useForgotPasswordForm } from "../hooks/useForgotPasswordForm";
import type { ForgotPasswordFormValues } from "../schemas/forgotPasswordForm";

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const form = useForgotPasswordForm();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = form;

  const [serverMessage, setServerMessage] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  const onSubmit = async (values: ForgotPasswordFormValues) => {
    setServerMessage(null);
    setServerError(null);

    try {
      await requestPasswordResetApi({ email: values.email });

      // 보안상 "존재하는 이메일인지"는 말하지 않고 항상 동일한 메시지
      setServerMessage(
        "If an account exists for this email, we've sent a password reset link."
      );
    } catch (err) {
      console.error("[ForgotPassword] request failed", err);
      setServerError("Failed to send reset link. Please try again.");
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: "40px auto" }}>
      <h1>Forgot Password</h1>
      <p style={{ marginBottom: 16 }}>
        Enter your email address and we'll send you a link to reset your password.
      </p>

      <form onSubmit={handleSubmit(onSubmit)}>
        {/* Email field */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Email
            <input
              type="email"
              {...register("email")}
              style={{ display: "block", width: "100%", marginTop: 4 }}
              placeholder="you@example.com"
            />
          </label>
          {/* Zod/RHF validation error */}
          {errors.email && (
            <div style={{ color: "red", fontSize: 12, marginTop: 4 }}>
              {errors.email.message}
            </div>
          )}
        </div>

        {/* Server-level error */}
        {serverError && (
          <div style={{ color: "red", marginBottom: 8 }}>{serverError}</div>
        )}

        {/* Success message */}
        {serverMessage && (
          <div style={{ color: "green", marginBottom: 8 }}>{serverMessage}</div>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          style={{ width: "100%" }}
        >
          {isSubmitting ? "Sending..." : "Send reset link"}
        </button>
      </form>

      <p style={{ marginTop: 16 }}>
        Remembered your password?{" "}
        <button
          type="button"
          onClick={() => navigate(ROUTES.LOGIN)}
          style={{
            border: "none",
            background: "none",
            color: "#0070f3",
            cursor: "pointer",
            padding: 0,
            textDecoration: "underline",
          }}
        >
          Back to login
        </button>
      </p>
    </div>
  );
}