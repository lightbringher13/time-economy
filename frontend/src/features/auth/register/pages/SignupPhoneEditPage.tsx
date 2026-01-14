// src/features/auth/register/pages/SignupPhoneEditPage.tsx
import { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";

import { SignupPhoneStep } from "../components/SignupPhoneStep";
import { useSignupFlow } from "../hooks/SignupFlowContext.tsx";

import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import type { SignupSessionState } from "../api/signupApi.types";
import { ROUTES } from "@/routes/paths.ts";

const EDIT_PHONE_ALLOWED: SignupSessionState[] = [
  "PROFILE_PENDING",
  "PROFILE_READY",
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

  // ---- route guard (edit-page exception rules) ----
  useEffect(() => {
    if (!state) return;

    if (EDIT_PHONE_ALLOWED.includes(state)) return;

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
  };

  const onIssueOtp = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");
    await onEditPhone(phone);
    await flow.sendPhoneOtp();
  };

  const onVerifyOtp = async (code: string) => {
    await flow.verifyPhoneOtp(code);
    const path = signupPathFromState(state);
    navigate(path);
  };

  const onBack = () => {
    navigate("/signup/edit/email");
  };

  const continueWithoutChange = () => {
    const target = signupPathFromState(state);
    navigate(target);
  };

  const onCancel = async () => {
    navigate(ROUTES.LOGIN,{replace: true});
    await flow.cancel();
    
  };

  // ---- UI rules ----
  const showOtpBox = view.state === "PHONE_OTP_SENT" && !view.phoneVerified;

  const showEdit = view.state === "PROFILE_PENDING" || view.state === "PROFILE_READY";

  const showSend =
    (view.state === "EMAIL_VERIFIED" || view.state === "PHONE_OTP_SENT") &&
    !view.phoneVerified;

  const sendLabel = view.state === "PHONE_OTP_SENT" ? "Resend code" : "Send SMS code";

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
          sendLabel,
          showOtpBox,
          showBack: true,
          showSkip,
          showCancel: true,
          backLabel: "Back",
          skipLabel: "Continue without changes",
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          edit: Boolean(flow.loading?.editPhone),
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          edit: onEditPhone,
          send: onIssueOtp,
          verify: onVerifyOtp,
          back: onBack,
          skip: continueWithoutChange,
          cancel: onCancel,
        }}
      />
    </div>
  );
}