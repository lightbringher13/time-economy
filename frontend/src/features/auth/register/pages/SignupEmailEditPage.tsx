// src/features/auth/register/pages/SignupEmailEditPage.tsx
import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { SignupEmailStep } from "../components/SignupEmailStep";
import { useSignupFlow } from "../hooks/useSignupFlow";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import type { SignupSessionState } from "../api/signupApi.types";

const EMAIL_EDIT_ALLOWED: SignupSessionState[] = [
  "DRAFT",
  "EMAIL_OTP_SENT",
  "EMAIL_VERIFIED",
  "PHONE_OTP_SENT",
  "PROFILE_PENDING",
  "PROFILE_READY",
];

export default function SignupEmailEditPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const flow = useSignupFlow();

  const emailForm = useSignupEmailForm();
  const otpForm = useSignupEmailOtpForm();

  // ---- bootstrap once ----
  const bootedRef = useRef(false);
  useEffect(() => {
    if (bootedRef.current) return;
    bootedRef.current = true;
    void flow.bootstrap();
  }, [flow.bootstrap]);

  const state = flow.view.state;

  // ---- route guard ----
  useEffect(() => {
    if (!state) return;

    if (EMAIL_EDIT_ALLOWED.includes(state)) return;

    // otherwise user is not allowed to be here -> go to correct page
    const path = signupPathFromState(state);
    if (path !== location.pathname) {
      navigate(path, { replace: true });
    }
  }, [state, navigate, location.pathname]);

  // ---- prefill email from server once ----
  useEffect(() => {
    const serverEmail = flow.view.email;
    if (serverEmail && !emailForm.getValues("email")) {
      emailForm.setValue("email", serverEmail, { shouldValidate: true });
    }
  }, [flow.view.email, emailForm]);

  // ---- handlers ----
  const cancel = async () => {
    await flow.cancel();
    navigate("/signup/email", { replace: true });
  };

  const editEmail = async (newEmail: string) => {
    await flow.editEmail(newEmail);
    otpForm.reset({ code: "" });
  };

  // unified "issue otp" (send + resend)
  const issueEmailOtp = async () => {
    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = emailForm.getValues("email");
    await editEmail(email);
    await flow.sendEmailOtp(); // same endpoint used for resend too
  };

  const verifyEmailOtp = async (code: string) => {
    await flow.verifyEmailOtp(code);
    const path = signupPathFromState(state);
    navigate(path, { replace: true });
  };

  const continueWithoutChange = () => {
    const target = signupPathFromState(state);
    navigate(target, { replace: true });
  };

  // ---- UI flags computed in the page ----
  const showOtpBox = state === "EMAIL_OTP_SENT" && !flow.view.emailVerified;

  // In edit page, allow issuing OTP anytime user is not verified
  const showSend = !flow.view.emailVerified;

  // Keep edit button only when you're not in initial draft/otp typing step
  const showEdit = state !== "DRAFT" && state !== "EMAIL_OTP_SENT";
  const showSkip = state !== "DRAFT" && state !== "EMAIL_OTP_SENT";

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
          title: "Edit email",
          subtitle: "Update your email and re-verify",
          showOtpBox,
          showSend,
          sendLabel,
          showEdit,
          showCancel: true,
          showSkip,
          skipLabel: "Continue without changes",
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
          skip: continueWithoutChange,
          cancel,
        }}
      />
    </div>
  );
}