// src/features/auth/register/pages/SignupEmailEditPage.tsx
import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { SignupEmailStep } from "../components/SignupEmailStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import type { SignupSessionState } from "../api/signupApi.types";
import { ROUTES } from "@/routes/paths";

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

  const state = flow.view.state;
  const from = (location.state as any)?.from as "review" | "phone" | undefined;

  const startUnlocked = from === "review";
  const [unlocked, setUnlocked] = useState<boolean>(startUnlocked);

  const exitTarget = useMemo(() => {
    if (from === "review") return ROUTES.SIGNUP_REVIEW;
    return ROUTES.SIGNUP_PHONE;
  }, [from]);

  const skipLabel = "Continue without changes";

  // ---- route guard ----
  useEffect(() => {
    if (!state) return;
    if (EMAIL_EDIT_ALLOWED.includes(state)) return;

    const path = signupPathFromState(state);
    if (path !== location.pathname) navigate(path, { replace: true });
  }, [state, navigate, location.pathname]);

  // prefill from server (don’t overwrite user edits)
  useEffect(() => {
    const serverEmail = flow.view.email;
    const dirty = emailForm.formState.dirtyFields;
    if (serverEmail && !dirty.email) {
      emailForm.setValue("email", serverEmail, { shouldValidate: true });
    }
  }, [flow.view.email, emailForm, emailForm.formState.dirtyFields]);

  const onCancel = async () => {
    navigate(ROUTES.LOGIN, { replace: true });
    await flow.cancel();
  };

  // exit (single button)
  const onContinueWithoutChange = () => {
    navigate(exitTarget, { replace: true });
  };

  const onUnlock = async () => {
    setUnlocked(true);
    otpForm.reset({ code: "" });
  };

  const onSend = async () => {
    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = emailForm.getValues("email");
    await flow.sendEmailOtp(email);

    setUnlocked(false);
    otpForm.reset({ code: "" });
  };

  const onVerify = async (code: string) => {
    await flow.verifyEmailOtp(code);
    const next = signupPathFromState(flow.view.state);
    navigate(next, { replace: true });
  };

  const showOtpBox = state === "EMAIL_OTP_SENT" && !flow.view.emailVerified;
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

          // unlock vs send
          showEdit: !unlocked,
          editLabel: "Change email",

          showSend: unlocked,
          sendLabel,

          // ✅ remove Back entirely
          showBack: false,

          // ✅ always show one exit button
          showSkip: true,
          skipLabel,

          showCancel: true,

          emailDisabled: !unlocked,
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          edit: false,
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          edit: onUnlock,
          send: onSend,
          verify: onVerify,

          // ✅ single exit handler
          skip: onContinueWithoutChange,

          cancel: onCancel,
        }}
      />
    </div>
  );
}