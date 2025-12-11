// src/features/auth/change-email/hooks/useChangeEmailApis.ts
import { useState } from "react";
import type { UseFormReturn } from "react-hook-form";

import {
  requestEmailChangeApi,
  verifyNewEmailCodeApi,
  verifySecondFactorApi,
  type SecondFactorType,
} from "../api/changeEmailApi";

import type {
  RequestEmailChangeFormValues,
  VerifyNewEmailCodeFormValues,
  VerifySecondFactorFormValues,
} from "../schemas/changeEmailSchemas";

type Step = 1 | 2 | 3 | 4; 
// 1: password + new email
// 2: new-email code
// 3: second factor
// 4: done

interface UseChangeEmailApisParams {
  step1Form: UseFormReturn<RequestEmailChangeFormValues>;
  step2Form: UseFormReturn<VerifyNewEmailCodeFormValues>;
  step3Form: UseFormReturn<VerifySecondFactorFormValues>;
}

export function useChangeEmailApis({
  step1Form,
  step2Form,
  step3Form,
}: UseChangeEmailApisParams) {
  const [step, setStep] = useState<Step>(1);

  const [requestId, setRequestId] = useState<number | null>(null);
  const [maskedNewEmail, setMaskedNewEmail] = useState<string | null>(null);
  const [secondFactorType, setSecondFactorType] =
    useState<SecondFactorType | null>(null);
  const [finalNewEmail, setFinalNewEmail] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // =========================
  // 1) Request email change
  // =========================
  const handleRequestSubmit = step1Form.handleSubmit(
    async (values: RequestEmailChangeFormValues) => {
      setError(null);
      setLoading(true);

      try {
        const res = await requestEmailChangeApi({
          currentPassword: values.currentPassword,
          newEmail: values.newEmail,
        });

        setRequestId(res.requestId);
        setMaskedNewEmail(res.maskedNewEmail);

        // optional: clear code fields for next steps
        step2Form.reset({ code: "" });
        step3Form.reset({ code: "" });

        setStep(2);
      } catch (err: any) {
        console.error("[ChangeEmail] requestEmailChange failed", err);

        const message =
          err?.response?.data?.message ??
          "이메일 변경 요청에 실패했습니다. 다시 시도해 주세요.";

        setError(message);

        // if you want to bind error to a specific field:
        // step1Form.setError("currentPassword", { type: "server", message });
      } finally {
        setLoading(false);
      }
    }
  );

  // ===================================
  // 2) Verify NEW-email code (step 2)
  // ===================================
  const handleVerifyNewEmailSubmit = step2Form.handleSubmit(
    async (values: VerifyNewEmailCodeFormValues) => {
      if (requestId == null) {
        setError("유효하지 않은 이메일 변경 요청입니다. 다시 시작해 주세요.");
        return;
      }

      setError(null);
      setLoading(true);

      try {
        const res = await verifyNewEmailCodeApi({
          requestId,
          code: values.code,
        });

        setSecondFactorType(res.secondFactorType);

        // clear step3 code before next
        step3Form.reset({ code: "" });

        setStep(3);
      } catch (err: any) {
        console.error("[ChangeEmail] verifyNewEmailCode failed", err);

        const message =
          err?.response?.data?.message ??
          "새 이메일 인증 코드가 올바르지 않거나 만료되었습니다.";

        setError(message);
        // step2Form.setError("code", { type: "server", message });
      } finally {
        setLoading(false);
      }
    }
  );

  // ============================================
  // 3) Verify second factor & commit (step 3)
  // ============================================
  const handleVerifySecondFactorSubmit = step3Form.handleSubmit(
    async (values: VerifySecondFactorFormValues) => {
      if (requestId == null) {
        setError("유효하지 않은 이메일 변경 요청입니다. 다시 시작해 주세요.");
        return;
      }

      setError(null);
      setLoading(true);

      try {
        const res = await verifySecondFactorApi({
          requestId,
          code: values.code,
        });

        setFinalNewEmail(res.newEmail);
        setStep(4);

        // Optionally reset forms
        step1Form.reset();
        step2Form.reset();
        step3Form.reset();
      } catch (err: any) {
        console.error("[ChangeEmail] verifySecondFactor failed", err);

        const message =
          err?.response?.data?.message ??
          "2차 인증 코드가 올바르지 않거나 만료되었습니다.";

        setError(message);
        // step3Form.setError("code", { type: "server", message });
      } finally {
        setLoading(false);
      }
    }
  );

  // =========================
  // Reset entire flow
  // =========================
  const resetFlow = () => {
    setStep(1);
    setRequestId(null);
    setMaskedNewEmail(null);
    setSecondFactorType(null);
    setFinalNewEmail(null);
    setError(null);

    step1Form.reset();
    step2Form.reset();
    step3Form.reset();
  };

  return {
    // state
    step,
    loading,
    error,
    requestId,
    maskedNewEmail,
    secondFactorType,
    finalNewEmail,

    // handlers (attach these to <form onSubmit={...}>)
    handleRequestSubmit,
    handleVerifyNewEmailSubmit,
    handleVerifySecondFactorSubmit,

    // reset
    resetFlow,
  };
}