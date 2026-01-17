// src/features/auth/register/pages/SignupPhonePage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { SignupPhoneStep } from "../components/forms/SignupPhoneStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";
import { useSignupShellUi } from "../hooks/SignupShellUiContext"; // ✅ UPDATED

import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import { ROUTES } from "@/routes/paths";

export default function SignupPhonePage() {
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi(); // ✅ UPDATED
  const navigate = useNavigate();

  const phoneForm = useSignupPhoneForm();
  const otpForm = useSignupPhoneOtpForm();

  const state = flow.view.state;

  // prefill from server, but don't overwrite user edits
  useEffect(() => {
    const serverPhone = flow.view.phoneNumber;
    const dirty = phoneForm.formState.dirtyFields;

    if (serverPhone && !dirty.phoneNumber) {
      phoneForm.setValue("phoneNumber", serverPhone, { shouldValidate: true });
    }
  }, [flow.view.phoneNumber, phoneForm, phoneForm.formState.dirtyFields]);

  // ✅ UPDATED: cancel uses shell modal (no immediate navigation/cancel here)
  const cancel = () => {
    shellUi.openCancelModal({
      reason: "user",
      title: "Cancel signup?",
      description: "Your signup progress will be discarded. You can start again anytime.",
      confirmLabel: "Cancel signup",
      cancelLabel: "Keep going",
      destructive: true,
      onConfirm: async () => {
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
  };

  const onBack = () => {
    // big-co: "Back" from phone goes to email edit (so user can change email if needed)
    navigate(ROUTES.SIGNUP_EMAIL_EDIT, { state: { from: "phone" } });
  };

  const onSend = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");
    await flow.sendPhoneOtp(phone);

    // clear code input each send/resend
    otpForm.reset({ code: "" });
  };

  const onVerify = async (code: string) => {
    await flow.verifyPhoneOtp(code);

    const next = signupPathFromState(flow.view.state);
    navigate(next, { replace: true });
  };

  // ---- UI rules (plain page) ----
  const isVerified = flow.view.phoneVerified;

  const showOtpBox = state === "PHONE_OTP_SENT" && !isVerified;

  // show send while not verified (EMAIL_VERIFIED or already pending otp)
  const showSend =
    !isVerified && (state === "EMAIL_VERIFIED" || state === "PHONE_OTP_SENT");

  const sendLabel = state === "PHONE_OTP_SENT" ? "Resend code" : "Send SMS code";

  /**
   * Big-co: lock phone input once OTP is pending OR once verified.
   * change phone happens in /signup/edit/phone
   */
  const phoneDisabled = isVerified || state === "PHONE_OTP_SENT";

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupPhoneStep
        phoneVerified={isVerified}
        maskedPhone={flow.view.phoneNumber}
        error={flow.error ?? undefined}
        phoneForm={phoneForm}
        otpForm={otpForm}
        ui={{
          title: "Create your account",
          subtitle: "Step 2 — Verify phone",

          showOtpBox,

          showSend,
          sendLabel,

          // ✅ plain page: no edit button
          showEdit: false,

          showBack: true,
          backLabel: "Back",

          showCancel: true,

          // ✅ lock rules
          phoneDisabled,
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          send: onSend,
          verify: onVerify,
          back: onBack,
          cancel, // ✅ UPDATED
        }}
      />
    </div>
  );
}