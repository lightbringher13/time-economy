// features/auth/pages/ChangePasswordPage.tsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { ROUTES } from "@/routes/paths";
import { changePasswordApi } from "../api/changePasswordApi";
import { useChangePasswordForm } from "../hooks/useChangePasswordForm";
import type { ChangePasswordFormValues } from "../schemas/changePasswordForm";
import type { ChangePasswordRequest } from "../api/changePasswordApi.type";

export default function ChangePasswordPage() {
  const navigate = useNavigate();

  const [serverError, setServerError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useChangePasswordForm();

  const onSubmit = async (values: ChangePasswordFormValues) => {
    setServerError(null);
    setSuccess(false);

    const payload: ChangePasswordRequest = {
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    };

    try {
      await changePasswordApi(payload);
      setSuccess(true);
      reset(); // 폼 비우기
    } catch (err: any) {
      console.error("[ChangePassword] failed", err);

      const msg =
        err?.response?.data?.message ??
        "Failed to change password. Please check your current password.";

      setServerError(msg);
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: "40px auto" }}>
      <h1>Change Password</h1>
      <p style={{ marginBottom: 16 }}>
        Enter your current password and choose a new one.
      </p>

      <form onSubmit={handleSubmit(onSubmit)}>
        {/* Current password */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Current password
            <input
              type="password"
              {...register("currentPassword")}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
          {errors.currentPassword && (
            <div style={{ color: "red", fontSize: 12 }}>
              {errors.currentPassword.message}
            </div>
          )}
        </div>

        {/* New password */}
        <div style={{ marginBottom: 12 }}>
          <label>
            New password
            <input
              type="password"
              {...register("newPassword")}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
          {errors.newPassword && (
            <div style={{ color: "red", fontSize: 12 }}>
              {errors.newPassword.message}
            </div>
          )}
        </div>

        {/* Confirm new password */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Confirm new password
            <input
              type="password"
              {...register("confirmNewPassword")}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
          {errors.confirmNewPassword && (
            <div style={{ color: "red", fontSize: 12 }}>
              {errors.confirmNewPassword.message}
            </div>
          )}
        </div>

        {/* 서버 비즈니스 에러 (현재 비밀번호 틀림 등) */}
        {serverError && (
          <div style={{ color: "red", marginBottom: 8 }}>{serverError}</div>
        )}

        {/* 성공 메시지 */}
        {success && (
          <div style={{ color: "green", marginBottom: 8 }}>
            Password changed successfully.
          </div>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          style={{ width: "100%" }}
        >
          {isSubmitting ? "Changing..." : "Change password"}
        </button>
      </form>

      <button
        type="button"
        onClick={() => navigate(ROUTES.PROFILE)}
        style={{
          marginTop: 16,
          border: "none",
          background: "none",
          color: "#0070f3",
          cursor: "pointer",
          textDecoration: "underline",
          padding: 0,
        }}
      >
        Back to profile
      </button>
    </div>
  );
}