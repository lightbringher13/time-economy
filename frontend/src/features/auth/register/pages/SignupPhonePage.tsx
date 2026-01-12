// src/features/auth/register/pages/SignupPhonePage.tsx
import { useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";

import { SignupPhoneStep } from "../components/SignupPhoneStep";
import { useSignupFlow } from "../hooks/useSignupFlow";

import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import type { SignupSessionState } from "../api/signupApi.types";

const PHONE_PAGE_ALLOWED: SignupSessionState[] = ["EMAIL_VERIFIED", "PHONE_OTP_SENT"];

export default function SignupPhonePage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();
  const location = useLocation();

  const phoneForm = useSignupPhoneForm();
  const otpForm = useSignupPhoneOtpForm();

  const state = flow.state; // canonical
  const view = flow.view;   // view-model

  // ---- bootstrap once ----
  const bootedRef = useRef(false);
  useEffect(() => {
    if (bootedRef.current) return;
    bootedRef.current = true;
    void flow.bootstrap();
  }, [flow.bootstrap]);

  // ---- route guard ----
  useEffect(() => {
    if (!state) return;

    if (PHONE_PAGE_ALLOWED.includes(state)) return;

    const path = signupPathFromState(state);
    if (path !== location.pathname) navigate(path, { replace: true });
  }, [state, navigate, location.pathname]);

  // ---- prefill phone ----
  useEffect(() => {
    const serverPhone = view.phoneNumber;
    if (serverPhone && !phoneForm.getValues("phoneNumber")) {
      phoneForm.setValue("phoneNumber", serverPhone, { shouldValidate: true });
    }
  }, [view.phoneNumber, phoneForm]);

  // ---- handlers ----
  const onEditPhone = async (newPhone: string) => {
    await flow.editPhone(newPhone);
    otpForm.reset({ code: "" });
  };

  // unified issue otp: send + resend
  const issuePhoneOtp = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");
    await onEditPhone(phone);   // persist first
    await flow.sendPhoneOtp();  // same endpoint used for resend too
  };

  const onVerifyOtp = async (code: string) => {
    await flow.verifyPhoneOtp(code);
    const path = signupPathFromState(state);
    navigate(path, { replace: true });
  };

  const onBackToEmail = () => {
    navigate("/signup/edit/email");
  };

  const onCancel = async () => {
    await flow.cancel();
    navigate("/signup/email", { replace: true });
  };

  // ---- UI rules decided by PAGE (component is dumb) ----
  const showOtpBox = view.state === "PHONE_OTP_SENT" && !view.phoneVerified;

  // show same button in both states (resend is just re-click)
  const showSend =
    (view.state === "EMAIL_VERIFIED" || view.state === "PHONE_OTP_SENT") &&
    !view.phoneVerified;

  const sendLabel = view.state === "PHONE_OTP_SENT" ? "Resend code" : "Send SMS code";

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupPhoneStep
        phoneVerified={view.phoneVerified}
        maskedPhone={view.phoneNumber}
        error={flow.error ?? undefined}
        phoneForm={phoneForm}
        otpForm={otpForm}
        ui={{
          title: "Create your account",
          subtitle: "Step 2 — Verify phone",
          showOtpBox,
          showSend,
          sendLabel,
          showEdit: false,   // phone page = normal flow (not edit)
          showBack: true,
          showSkip: false,
          showCancel: true,
          backLabel: "Back",
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          edit: false,
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          send: issuePhoneOtp,
          verify: onVerifyOtp,
          back: onBackToEmail,
          cancel: onCancel,
        }}
      />
    </div>
  );
}