// src/features/auth/register/pages/SignupEmailPage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { SignupEmailStep } from "../components/forms/SignupEmailStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";
import { useSignupShellUi } from "../hooks/SignupShellUiContext";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import { ROUTES } from "@/routes/paths";

export default function SignupEmailPage() {
  const navigate = useNavigate();
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi();

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

  const onCancel = () => {
    shellUi.openCancelModal({
      reason: "user",
      title: "Cancel signup?",
      description: "Your signup progress will be discarded. You can start again anytime.",
      confirmLabel: "Cancel signup",
      cancelLabel: "Keep going",
      destructive: true,
      onConfirm: async () => {
        // leave signup tree first so layout effects stop fighting navigation
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
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

  // lock email input once OTP is pending OR once verified
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
          showEdit: false,
          showBack: false,
          showCancel: true,
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
          cancel: onCancel, // ✅ modal now
        }}
      />
    </div>
  );
}