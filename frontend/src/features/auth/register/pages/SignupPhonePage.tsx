// src/features/auth/register/pages/SignupPhonePage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { SignupPhoneStep } from "../components/SignupPhoneStep";
import { useSignupFlow } from "../hooks/SignupFlowContext.tsx";

import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import { ROUTES } from "@/routes/paths.ts";

export default function SignupPhonePage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();

  const phoneForm = useSignupPhoneForm();
  const otpForm = useSignupPhoneOtpForm();

  const state = flow.state; // canonical
  const view = flow.view;   // view-model

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

  const issuePhoneOtp = async () => {
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

  const onBackToEmail = () => {
    navigate("/signup/edit/email");
  };

  const onCancel = async () => {
    navigate(ROUTES.LOGIN,{replace: true});
    await flow.cancel();
    
  };

  // ---- UI rules ----
  const showOtpBox = view.state === "PHONE_OTP_SENT" && !view.phoneVerified;

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
          showEdit: false,
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