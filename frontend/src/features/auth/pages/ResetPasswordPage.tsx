// features/auth/pages/ResetPasswordPage.tsx
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useResetPasswordForm } from "../hooks/useResetPasswordForm";
import { confirmPasswordResetApi } from "../api/authApi";
import { ROUTES } from "@/routes/paths";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";

  const [serverError, setServerError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useResetPasswordForm();

  const onSubmit = async (values: { password: string; passwordConfirm: string }) => {
    setServerError(null);

    if (!token) {
      setServerError("Invalid or missing reset token.");
      return;
    }

    try {
      await confirmPasswordResetApi(token, {
        newPassword: values.password,
        confirmPassword: values.passwordConfirm,
      });

      setSuccess(true);
    } catch (err) {
      console.error("[ResetPassword] confirm failed", err);
      setServerError("Failed to reset password. The link may have expired.");
    }
  };

  // 토큰 아예 없으면 폼 대신 에러만 보여줄 수도 있음
  if (!token) {
    return (
      <div style={{ maxWidth: 400, margin: "40px auto" }}>
        <h1>Reset Password</h1>
        <p style={{ color: "red" }}>
          Reset token is missing or invalid. Please request a new reset link.
        </p>
        <button onClick={() => navigate(ROUTES.LOGIN)}>Back to Login</button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 400, margin: "40px auto" }}>
      <h1>Reset Password</h1>
      <p style={{ marginBottom: 16 }}>
        Please enter your new password.
      </p>

      {success ? (
        <>
          <p style={{ marginBottom: 16 }}>
            Your password has been reset successfully.
          </p>
          <button
            type="button"
            onClick={() => navigate(ROUTES.LOGIN)}
            style={{ width: "100%" }}
          >
            Go to Login
          </button>
        </>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)}>
          {/* Password */}
          <div style={{ marginBottom: 12 }}>
            <label>
              New Password
              <input
                type="password"
                {...register("password")}
                style={{ display: "block", width: "100%", marginTop: 4 }}
              />
            </label>
            {errors.password && (
              <div style={{ color: "red", marginTop: 4 }}>
                {errors.password.message}
              </div>
            )}
          </div>

          {/* Confirm Password */}
          <div style={{ marginBottom: 12 }}>
            <label>
              Confirm Password
              <input
                type="password"
                {...register("passwordConfirm")}
                style={{ display: "block", width: "100%", marginTop: 4 }}
              />
            </label>
            {errors.passwordConfirm && (
              <div style={{ color: "red", marginTop: 4 }}>
                {errors.passwordConfirm.message}
              </div>
            )}
          </div>

          {serverError && (
            <div style={{ color: "red", marginBottom: 8 }}>{serverError}</div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            style={{ width: "100%" }}
          >
            {isSubmitting ? "Resetting..." : "Reset Password"}
          </button>
        </form>
      )}
    </div>
  );
}