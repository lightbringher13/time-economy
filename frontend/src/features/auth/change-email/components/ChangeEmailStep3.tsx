// src/features/auth/change-email/components/ChangeEmailStep3.tsx
import React from "react";
import type { UseFormReturn } from "react-hook-form";
import type { VerifySecondFactorFormValues } from "../schemas/changeEmailSchemas";
import type { SecondFactorType } from "../api/changeEmailApi";

interface ChangeEmailStep3Props {
  form: UseFormReturn<VerifySecondFactorFormValues>;
  onSubmit: (e: React.FormEvent<HTMLFormElement>) => void; // handleVerifySecondFactorSubmit
  loading: boolean;
  error?: string | null;
  secondFactorType: SecondFactorType | null;

  // Optional masked info to display
  maskedPhoneNumber?: string | null;   // e.g. ***-****-1234
  maskedOldEmail?: string | null;      // e.g. o***@gmail.com
}

export const ChangeEmailStep3: React.FC<ChangeEmailStep3Props> = ({
  form,
  onSubmit,
  loading,
  error,
  secondFactorType,
  maskedPhoneNumber,
  maskedOldEmail,
}) => {
  const {
    register,
    formState: { errors },
  } = form;

  const renderInstruction = () => {
    if (secondFactorType === "PHONE") {
      return (
        <>
          휴대폰으로 전송된 6자리 코드를 입력해 주세요.
          <br />
          대상: <strong>{maskedPhoneNumber ?? "등록된 휴대폰 번호"}</strong>
        </>
      );
    }

    if (secondFactorType === "OLD_EMAIL") {
      return (
        <>
          기존 이메일로 전송된 6자리 코드를 입력해 주세요.
          <br />
          대상: <strong>{maskedOldEmail ?? "기존 이메일"}</strong>
        </>
      );
    }

    // Fallback (should not normally happen)
    return <>2차 인증 코드를 입력해 주세요.</>;
  };

  return (
    <form onSubmit={onSubmit} noValidate>
      <h2>2차 인증</h2>

      <p style={{ marginBottom: "0.75rem", fontSize: "0.9rem" }}>
        {renderInstruction()}
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
          htmlFor="secondFactorCode"
          style={{ display: "block", marginBottom: "0.25rem" }}
        >
          인증 코드
        </label>
        <input
          id="secondFactorCode"
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

      <p style={{ fontSize: "0.8rem", color: "#666", marginBottom: "1rem" }}>
        보안을 위해 이메일 변경 시 2차 인증이 필요합니다.
      </p>

      <button
        type="submit"
        disabled={loading}
        style={{ padding: "0.5rem 1rem" }}
      >
        {loading ? "확인 중..." : "이메일 변경 완료"}
      </button>
    </form>
  );
};