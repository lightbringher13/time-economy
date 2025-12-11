// src/features/auth/change-email/components/ChangeEmailStep2.tsx
import React from "react";
import type { UseFormReturn } from "react-hook-form";
import type { VerifyNewEmailCodeFormValues } from "../schemas/changeEmailSchemas";

interface ChangeEmailStep2Props {
  form: UseFormReturn<VerifyNewEmailCodeFormValues>;
  onSubmit: (e: React.FormEvent<HTMLFormElement>) => void; // handleVerifyNewEmailSubmit
  loading: boolean;
  error?: string | null;
  maskedNewEmail?: string | null;

  // optional resend support (parent can implement later)
  onResendClick?: () => void;
  resendDisabled?: boolean;
  resendSecondsLeft?: number;
}

export const ChangeEmailStep2: React.FC<ChangeEmailStep2Props> = ({
  form,
  onSubmit,
  loading,
  error,
  maskedNewEmail,
  onResendClick,
  resendDisabled,
  resendSecondsLeft,
}) => {
  const {
    register,
    formState: { errors },
  } = form;

  const resendLabel =
    resendSecondsLeft && resendSecondsLeft > 0
      ? `재전송 (${resendSecondsLeft}s)`
      : "코드 재전송";

  return (
    <form onSubmit={onSubmit} noValidate>
      <h2>새 이메일 인증</h2>

      <p style={{ marginBottom: "0.75rem", fontSize: "0.9rem" }}>
        새 이메일로 전송된 6자리 코드를 입력해 주세요.
        <br />
        대상:{" "}
        <strong>{maskedNewEmail ?? "새 이메일"}</strong>
      </p>

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

      <div style={{ marginBottom: "1rem" }}>
        <label
          htmlFor="newEmailCode"
          style={{ display: "block", marginBottom: "0.25rem" }}
        >
          인증 코드
        </label>
        <input
          id="newEmailCode"
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          {...register("code")}
          disabled={loading}
          style={{ width: "100%", padding: "0.5rem" }}
        />
        {errors.code && (
          <div
            style={{ color: "red", fontSize: "0.8rem", marginTop: "0.25rem" }}
          >
            {errors.code.message}
          </div>
        )}
      </div>

      <div
        style={{
          display: "flex",
          gap: "0.5rem",
          alignItems: "center",
          marginBottom: "1rem",
        }}
      >
        <button
          type="submit"
          disabled={loading}
          style={{ padding: "0.5rem 1rem" }}
        >
          {loading ? "검증 중..." : "코드 확인"}
        </button>

        {onResendClick && (
          <button
            type="button"
            onClick={onResendClick}
            disabled={loading || resendDisabled}
            style={{
              padding: "0.5rem 0.75rem",
              fontSize: "0.8rem",
            }}
          >
            {resendLabel}
          </button>
        )}
      </div>

      <p style={{ fontSize: "0.8rem", color: "#666" }}>
        메일이 보이지 않는다면 스팸함도 확인해 주세요.
      </p>
    </form>
  );
};