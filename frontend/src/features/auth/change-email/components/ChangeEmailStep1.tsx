// src/features/auth/change-email/components/ChangeEmailStep1.tsx
import React from "react";
import type { UseFormReturn } from "react-hook-form";
import type { RequestEmailChangeFormValues } from "../schemas/changeEmailSchemas";

interface ChangeEmailStep1Props {
  form: UseFormReturn<RequestEmailChangeFormValues>;
  onSubmit: (e: React.FormEvent<HTMLFormElement>) => void; // from useChangeEmailApis.handleRequestSubmit
  loading: boolean;
  error?: string | null;
  currentEmail?: string; // optional, if you want to show it
}

export const ChangeEmailStep1: React.FC<ChangeEmailStep1Props> = ({
  form,
  onSubmit,
  loading,
  error,
  currentEmail,
}) => {
  const {
    register,
    formState: { errors },
  } = form;

  return (
    <form onSubmit={onSubmit} noValidate>
      <h2>이메일 변경</h2>

      {currentEmail && (
        <div style={{ marginBottom: "0.75rem", fontSize: "0.9rem" }}>
          현재 이메일: <strong>{currentEmail}</strong>
        </div>
      )}

      {error && (
        <div
          style={{
            marginBottom: "0.75rem",
            color: "red",
            fontSize: "0.9rem",
          }}
        >
          {error}
        </div>
      )}

      {/* Current password */}
      <div style={{ marginBottom: "1rem" }}>
        <label
          htmlFor="currentPassword"
          style={{ display: "block", marginBottom: "0.25rem" }}
        >
          현재 비밀번호
        </label>
        <input
          id="currentPassword"
          type="password"
          autoComplete="current-password"
          {...register("currentPassword")}
          disabled={loading}
          style={{ width: "100%", padding: "0.5rem" }}
        />
        {errors.currentPassword && (
          <div style={{ color: "red", fontSize: "0.8rem", marginTop: "0.25rem" }}>
            {errors.currentPassword.message}
          </div>
        )}
      </div>

      {/* New email */}
      <div style={{ marginBottom: "1rem" }}>
        <label
          htmlFor="newEmail"
          style={{ display: "block", marginBottom: "0.25rem" }}
        >
          새 이메일
        </label>
        <input
          id="newEmail"
          type="email"
          autoComplete="email"
          {...register("newEmail")}
          disabled={loading}
          style={{ width: "100%", padding: "0.5rem" }}
        />
        {errors.newEmail && (
          <div style={{ color: "red", fontSize: "0.8rem", marginTop: "0.25rem" }}>
            {errors.newEmail.message}
          </div>
        )}
      </div>

      {/* Helper text */}
      <p style={{ fontSize: "0.8rem", color: "#666", marginBottom: "1rem" }}>
        보안을 위해 현재 비밀번호를 확인한 후, 새 이메일 소유를 인증합니다.
      </p>

      <button type="submit" disabled={loading} style={{ padding: "0.5rem 1rem" }}>
        {loading ? "요청 중..." : "인증 코드 보내기"}
      </button>
    </form>
  );
};