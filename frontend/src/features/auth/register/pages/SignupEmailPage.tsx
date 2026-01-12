// src/features/auth/register/pages/SignupEmailPage.tsx
import { useEffect, useMemo, useRef } from "react";
import { useNavigate } from "react-router-dom";

import { SignupEmailStep } from "../components/SignupEmailStep";
import { useSignupFlow } from "../hooks/useSignupFlow";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";

export default function SignupEmailPage() {
  const navigate = useNavigate();
  const flow = useSignupFlow();

  const emailForm = useSignupEmailForm();
  const otpForm = useSignupEmailOtpForm();

  // ---- bootstrap once (creates/refreshes session cookie) ----
  const bootedRef = useRef(false);
  useEffect(() => {
    if (bootedRef.current) return;
    bootedRef.current = true;
    void flow.bootstrap();
  }, [flow.bootstrap]);

  // ---- route guard: if server state says "not email step", redirect ----
  const state = flow.view.state;
  const expectedPath = useMemo(() => signupPathFromState(state), [state]);

  useEffect(() => {
    if (!state) return;
    if (expectedPath !== "/signup/email") {
      navigate(expectedPath, { replace: true });
    }
  }, [expectedPath, state, navigate]);

  // ---- prefill email from server once ----
  useEffect(() => {
    const serverEmail = flow.view.email;
    if (serverEmail && !emailForm.getValues("email")) {
      emailForm.setValue("email", serverEmail, { shouldValidate: true });
    }
  }, [flow.view.email, emailForm]);

  // ---- handlers (same logic as before) ----
  const cancel = async () => {
    await flow.cancel();
    // after cancel, state becomes CANCELED -> guard redirects
  };

  const editEmail = async (newEmail: string) => {
    await flow.editEmail(newEmail);
    otpForm.reset({ code: "" });
  };

  const sendEmailOtp = async () => {
    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = emailForm.getValues("email");
    await editEmail(email);     // persist first
    await flow.sendEmailOtp();  // then send
  };

  const resendEmailOtp = async () => {
    otpForm.reset({ code: "" });
    await flow.resendEmailOtp();
  };

  const verifyEmailOtp = async (code: string) => {
    await flow.verifyEmailOtp(code);
    const path = signupPathFromState(state);
    navigate(path, { replace: true });
  };

  // ---- UI flags computed here (page decides) ----
  const showOtpBox = state === "EMAIL_OTP_SENT" && !flow.view.emailVerified;
  const showSend = state === "DRAFT" && !flow.view.emailVerified;
  const showResend = state === "EMAIL_OTP_SENT" && !flow.view.emailVerified;

  // Optional: show edit in this page or move edit to /signup/edit/email
  const showEdit = state === "EMAIL_OTP_SENT" || state === "EMAIL_VERIFIED";

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
          showResend,
          showEdit,      // set false if you want edit only in /signup/edit/email
          showCancel: true,
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          resend: Boolean(flow.loading?.resendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          edit: Boolean(flow.loading?.editEmail),
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          send: sendEmailOtp,
          resend: resendEmailOtp,
          verify: verifyEmailOtp,
          edit: editEmail,
          cancel,
        }}
      />
    </div>
  );
}