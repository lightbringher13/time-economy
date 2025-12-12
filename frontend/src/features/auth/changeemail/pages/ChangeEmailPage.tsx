// src/features/auth/change-email/pages/ChangeEmailPage.tsx
import React from "react";
import { useNavigate } from "react-router-dom";

import { useRequestEmailChangeForm } from "../hooks/useRequestEmailChangeForm";
import { useVerifyNewEmailCodeForm } from "../hooks/useVerifyNewEmailCodeForm";
import { useVerifySecondFactorForm } from "../hooks/useVerifySecondFactorForm";
import { useChangeEmailApis } from "../hooks/useChangeEmailApis";

import { ChangeEmailStep1 } from "../components/ChangeEmailStep1";
import { ChangeEmailStep2 } from "../components/ChangeEmailStep2";
import { ChangeEmailStep3 } from "../components/ChangeEmailStep3";
import { ChangeEmailSuccessStep } from "../components/ChangeEmailSuccessStep";
// import { useAuth } from "@/features/auth/hooks/useAuth"; // TODO: your real auth hook

// Simple mask helpers (you can move these to a utils file)
function maskEmail(email: string | null | undefined): string | null {
  if (!email) return null;
  const atIdx = email.indexOf("@");
  if (atIdx <= 1) return "***" + email.slice(atIdx);
  return email[0] + "***" + email.slice(atIdx);
}

function maskPhone(phone: string | null | undefined): string | null {
  if (!phone) return null;
  // very naive example: keep last 4 digits
  const digits = phone.replace(/\D/g, "");
  if (digits.length <= 4) return "****" + digits;
  const last4 = digits.slice(-4);
  return "***-****-" + last4;
}

export const ChangeEmailPage: React.FC = () => {
  const navigate = useNavigate();

  // TODO: plug in your real auth state
  // const { user, logout } = useAuth();
  const user = {
    email: "current@example.com",
    phoneNumber: "010-1234-5678",
    phoneVerified: true,
  }; // placeholder; remove when you use real data

  const currentEmail = user?.email ?? null;
  const maskedCurrentEmail = maskEmail(currentEmail);
  const maskedPhoneNumber = user?.phoneVerified
    ? maskPhone(user.phoneNumber)
    : null;

  // 1) RHF forms for each step
  const step1Form = useRequestEmailChangeForm();
  const step2Form = useVerifyNewEmailCodeForm();
  const step3Form = useVerifySecondFactorForm();

  // 2) Change-email flow logic + API calls
  const {
    step,
    loading,
    error,
    maskedNewEmail,
    secondFactorType,
    finalNewEmail,
    handleRequestSubmit,
    handleVerifyNewEmailSubmit,
    handleVerifySecondFactorSubmit,
    resetFlow,
  } = useChangeEmailApis({ step1Form, step2Form, step3Form });

  // 3) Handlers for success page
  const handleLogoutRedirect = () => {
    // TODO: clear auth state + redirect to login
    // e.g.
    // logout();
    // navigate("/login", { replace: true });
    navigate("/login", { replace: true });
  };

  const handleClose = () => {
    // e.g. go back to settings page or home
    resetFlow();
    navigate("/settings/account");
  };

  return (
    <div
      style={{
        maxWidth: 480,
        margin: "0 auto",
        padding: "1.5rem",
      }}
    >
      {/* Optional simple step indicator */}
      <div style={{ marginBottom: "1rem", fontSize: "0.85rem", color: "#555" }}>
        {step === 1 && "1 / 3 - 비밀번호 확인 및 새 이메일 입력"}
        {step === 2 && "2 / 3 - 새 이메일 인증"}
        {step === 3 && "3 / 3 - 2차 인증"}
        {step === 4 && "완료"}
      </div>

      {step === 1 && (
        <ChangeEmailStep1
          form={step1Form}
          onSubmit={handleRequestSubmit}
          loading={loading}
          error={error}
          currentEmail={maskedCurrentEmail ?? undefined}
        />
      )}

      {step === 2 && (
        <ChangeEmailStep2
          form={step2Form}
          onSubmit={handleVerifyNewEmailSubmit}
          loading={loading}
          error={error}
          maskedNewEmail={maskedNewEmail}
          // TODO: wire resend logic later if you want
          // onResendClick={handleResendNewEmailCode}
          // resendDisabled={isResendDisabled}
          // resendSecondsLeft={resendSeconds}
        />
      )}

      {step === 3 && (
        <ChangeEmailStep3
          form={step3Form}
          onSubmit={handleVerifySecondFactorSubmit}
          loading={loading}
          error={error}
          secondFactorType={secondFactorType}
          maskedPhoneNumber={maskedPhoneNumber}
          maskedOldEmail={maskedCurrentEmail}
        />
      )}

      {step === 4 && (
        <ChangeEmailSuccessStep
          newEmail={finalNewEmail}
          onLogoutRedirect={handleLogoutRedirect}
          onClose={handleClose}
        />
      )}
    </div>
  );
};