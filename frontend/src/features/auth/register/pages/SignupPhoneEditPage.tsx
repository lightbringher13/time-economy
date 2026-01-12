// src/features/auth/register/pages/SignupPhoneEditPage.tsx
import { useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";

import { SignupPhoneStep } from "../components/SignupPhoneStep";
import { useSignupFlow } from "../hooks/useSignupFlow";

import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import type { SignupSessionState } from "../api/signupApi.types";

const EDIT_PHONE_ALLOWED: SignupSessionState[] = [
  // where user came from
  "PROFILE_PENDING",
  "PROFILE_READY",

  // ✅ edit-phone flow will temporarily move backwards:
  "EMAIL_VERIFIED",
  "PHONE_OTP_SENT",
];

export default function SignupPhoneEditPage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();
  const location = useLocation();

  const phoneForm = useSignupPhoneForm();
  const otpForm = useSignupPhoneOtpForm();

  const state = flow.state;
  const view = flow.view;

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

    // allow staying on edit page during the whole edit flow
    if (EDIT_PHONE_ALLOWED.includes(state)) return;

    // otherwise redirect to the canonical page for that state
    const path = signupPathFromState(state);
    if (path !== location.pathname) navigate(path, { replace: true });
  }, [state, navigate, location.pathname]);

  // ---- prefill phone (when empty) ----
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
    // After editPhone, state may become EMAIL_VERIFIED (allowed here).
  };

  const onSendOtp = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");
    await onEditPhone(phone);      // persist first (moves state back)
    await flow.sendPhoneOtp();     // then send (state -> PHONE_OTP_SENT)
  };

  const onResendOtp = async () => {
    otpForm.reset({ code: "" });
    await flow.resendPhoneOtp();
  };

  const onVerifyOtp = async (code: string) => {
    await flow.verifyPhoneOtp(code);
    const path = signupPathFromState(state);
    navigate(path, { replace: true });
  };

  const onBack = () => {
    navigate("/signup/edit/email");
  }

  const continueWithoutChange = () => {
    // if you want “Continue” to go back to current step
    const target = signupPathFromState(state);
    navigate(target, { replace: true });
  };

  const onCancel = async () => {
    await flow.cancel();
    navigate("/signup/email", { replace: true });
  };

  // ---- UI rules (page decides, component is dumb) ----
  const showOtpBox = view.state === "PHONE_OTP_SENT" && !view.phoneVerified;

  // In edit flow:
  // - user must click "Update phone" first while in PROFILE_* (so state becomes EMAIL_VERIFIED)
  // - then "Send SMS code" becomes available
  const showEdit = view.state === "PROFILE_PENDING" || view.state === "PROFILE_READY";
  const showSend = view.state === "EMAIL_VERIFIED" && !view.phoneVerified;
  const showResend = view.state === "PHONE_OTP_SENT" && !view.phoneVerified;
  const showSkip = view.state !== "EMAIL_VERIFIED" && view.state !== "PHONE_OTP_SENT";

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupPhoneStep
        phoneVerified={view.phoneVerified}
        maskedPhone={view.phoneNumber}
        error={flow.error ?? undefined}
        phoneForm={phoneForm}
        otpForm={otpForm}
        ui={{
          title: "Edit phone number",
          subtitle: "Update your phone and verify",
          showEdit,
          showSend,
          showResend,
          showOtpBox,
          showBack: true,
          showSkip,
          showCancel: true,
          backLabel: "Back",
          skipLabel: "Continue without changes",
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          resend: Boolean(flow.loading?.resendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          edit: Boolean(flow.loading?.editPhone),
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          edit: onEditPhone,
          send: onSendOtp,
          resend: onResendOtp,
          verify: onVerifyOtp,
          back: onBack,
          skip: continueWithoutChange,
          cancel: onCancel,
        }}
      />
    </div>
  );
}