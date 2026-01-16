// src/features/auth/register/pages/SignupEmailPage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { SignupEmailStep } from "../components/SignupEmailStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import { ROUTES } from "@/routes/paths";

export default function SignupEmailPage() {
  const navigate = useNavigate();
  const flow = useSignupFlow();

  const emailForm = useSignupEmailForm();
  const otpForm = useSignupEmailOtpForm();

  const state = flow.view.state;

  // prefill from server, but don't overwrite user edits
  useEffect(() => {
    const serverEmail = flow.view.email;
    const dirty = emailForm.formState.dirtyFields;

    if (serverEmail && !dirty.email) {
      emailForm.setValue("email", serverEmail, { shouldValidate: true });
    }
  }, [flow.view.email, emailForm, emailForm.formState.dirtyFields]);

  const cancel = async () => {
    navigate(ROUTES.LOGIN, { replace: true });
    await flow.cancel();
  };

  const onSend = async () => {
    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = emailForm.getValues("email");
    await flow.sendEmailOtp(email);

    // clear code input each send/resend
    otpForm.reset({ code: "" });
  };

  const onVerify = async (code: string) => {
    await flow.verifyEmailOtp(code);

    // go canonical after verify (likely /signup/phone)
    const next = signupPathFromState(flow.view.state);
    navigate(next, { replace: true });
  };

  // ---- UI rules (plain page) ----
  const isVerified = flow.view.emailVerified;

  const showOtpBox = state === "EMAIL_OTP_SENT" && !isVerified;

  // show send while not verified (draft or otp sent)
  const showSend = !isVerified && (state === "DRAFT" || state === "EMAIL_OTP_SENT");
  const sendLabel = state === "EMAIL_OTP_SENT" ? "Resend code" : "Send code";

  /**
   * Big-co: lock email input once OTP is pending OR once verified.
   * - prevents mismatch while user is typing code
   * - “change email” happens in edit page, not here
   */
  const emailDisabled = isVerified || state === "EMAIL_OTP_SENT";

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupEmailStep
        emailVerified={isVerified}
        maskedEmail={flow.view.email}
        error={flow.error ?? undefined}
        emailForm={emailForm}
        otpForm={otpForm}
        ui={{
          title: "Create your account",
          subtitle: "Step 1 — Verify email",

          showOtpBox,

          showSend,
          sendLabel,

          // ✅ plain page: no edit button
          showEdit: false,

          // optional: no back here (first step)
          showBack: false,

          showCancel: true,

          // ✅ lock rules
          emailDisabled,
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          send: onSend,
          verify: onVerify,
          cancel,
        }}
      />
    </div>
  );
}