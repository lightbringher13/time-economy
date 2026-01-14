// src/features/auth/register/pages/SignupEmailPage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { SignupEmailStep } from "../components/SignupEmailStep";
import { useSignupFlow } from "../hooks/SignupFlowContext.tsx";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import { ROUTES } from "@/routes/paths.ts";

export default function SignupEmailPage() {
  const navigate = useNavigate();
  const flow = useSignupFlow();

  const emailForm = useSignupEmailForm();
  const otpForm = useSignupEmailOtpForm();

  const state = flow.view.state;

  // ---- prefill email from server once ----
  useEffect(() => {
    const serverEmail = flow.view.email;
    if (serverEmail && !emailForm.getValues("email")) {
      emailForm.setValue("email", serverEmail, { shouldValidate: true });
    }
  }, [flow.view.email, emailForm]);

  // ---- handlers ----
  const cancel = async () => {
    navigate(ROUTES.LOGIN,{replace: true});
    await flow.cancel();
    
  };

  const editEmail = async (newEmail: string) => {
    await flow.editEmail(newEmail);
    otpForm.reset({ code: "" });
  };

  const issueEmailOtp = async () => {
    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = emailForm.getValues("email");
    await editEmail(email);
    await flow.sendEmailOtp();
  };

  const verifyEmailOtp = async (code: string) => {
    await flow.verifyEmailOtp(code);
    const path = signupPathFromState(state);
    navigate(path);
  };

  // ---- UI flags computed here ----
  const showOtpBox = state === "EMAIL_OTP_SENT" && !flow.view.emailVerified;

  const showSend =
    (state === "DRAFT" || state === "EMAIL_OTP_SENT") && !flow.view.emailVerified;

  const showEdit = state === "EMAIL_OTP_SENT" || state === "EMAIL_VERIFIED";

  const sendLabel = state === "EMAIL_OTP_SENT" ? "Resend code" : "Send code";

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupEmailStep
        emailVerified={flow.view.emailVerified}
        maskedEmail={flow.view.email}
        error={flow.error ?? undefined}
        emailForm={emailForm}
        otpForm={otpForm}
        ui={{
          showOtpBox,
          showSend,
          sendLabel,
          showEdit,
          showCancel: true,
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          edit: Boolean(flow.loading?.editEmail),
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          send: issueEmailOtp,
          verify: verifyEmailOtp,
          edit: editEmail,
          cancel,
        }}
      />
    </div>
  );
}