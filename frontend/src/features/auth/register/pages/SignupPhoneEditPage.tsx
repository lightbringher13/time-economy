// src/features/auth/register/pages/SignupPhoneEditPage.tsx
import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { SignupPhoneStep } from "../components/SignupPhoneStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";

import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";

import { signupPathFromState } from "../routes/signupRouteMap";
import type { SignupSessionState } from "../api/signupApi.types";
import { ROUTES } from "@/routes/paths";

const PHONE_EDIT_ALLOWED: SignupSessionState[] = [
  "EMAIL_VERIFIED",
  "PHONE_OTP_SENT",
  "PROFILE_PENDING",
  "PROFILE_READY",
];

export default function SignupPhoneEditPage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();
  const location = useLocation();

  const phoneForm = useSignupPhoneForm();
  const otpForm = useSignupPhoneOtpForm();

  const state = flow.view.state;

  // where did user come from?
  // (set this when navigating: navigate(ROUTES.SIGNUP_PHONE_EDIT, { state: { from: "review" } }))
  const from = (location.state as any)?.from as "review" | "profile" | undefined;

  // ✅ match email edit page behavior
  // - from review => start unlocked
  // - otherwise => start locked
  const startUnlocked = from === "review";
  const [unlocked, setUnlocked] = useState<boolean>(startUnlocked);

  // ✅ single exit target (like email edit page)
  const exitTarget = useMemo(() => {
    if (from === "review") return ROUTES.SIGNUP_REVIEW;
    if (from === "profile") return ROUTES.SIGNUP_PROFILE;
    // default: phone edit is usually reached from profile step going back
    return ROUTES.SIGNUP_PROFILE;
  }, [from]);

  const skipLabel = "Continue without changes";

  // ---- route guard ----
  useEffect(() => {
    if (!state) return;
    if (PHONE_EDIT_ALLOWED.includes(state)) return;

    const path = signupPathFromState(state);
    if (path !== location.pathname) {
      navigate(path, { replace: true });
    }
  }, [state, navigate, location.pathname]);

  // ---- prefill phone (don’t overwrite user edits) ----
  useEffect(() => {
    const serverPhone = flow.view.phoneNumber;
    const dirty = phoneForm.formState.dirtyFields;

    if (serverPhone && !dirty.phoneNumber) {
      phoneForm.setValue("phoneNumber", serverPhone, { shouldValidate: true });
    }
  }, [flow.view.phoneNumber, phoneForm, phoneForm.formState.dirtyFields]);

  const onCancel = async () => {
    navigate(ROUTES.LOGIN, { replace: true });
    await flow.cancel();
  };

  // ✅ single exit handler (replaces Back + Skip naming)
  const onContinueWithoutChange = () => {
    navigate(exitTarget, { replace: true });
  };

  // ✅ unlock (enable input + show send button)
  const onUnlock = async () => {
    setUnlocked(true);
    otpForm.reset({ code: "" });
  };

  const onSend = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");
    await flow.sendPhoneOtp(phone);

    // ✅ lock again after sending to avoid mismatch while verifying
    setUnlocked(false);

    otpForm.reset({ code: "" });
  };

  const onVerify = async (code: string) => {
    await flow.verifyPhoneOtp(code);

    const next = signupPathFromState(flow.view.state);
    navigate(next, { replace: true });
  };

  // ---- UI rules ----
  const showOtpBox = state === "PHONE_OTP_SENT" && !flow.view.phoneVerified;
  const sendLabel = state === "PHONE_OTP_SENT" ? "Resend code" : "Send SMS code";

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupPhoneStep
        phoneVerified={flow.view.phoneVerified}
        maskedPhone={flow.view.phoneNumber}
        error={flow.error ?? undefined}
        phoneForm={phoneForm}
        otpForm={otpForm}
        ui={{
          title: "Edit phone number",
          subtitle: "Update your phone and re-verify",

          showOtpBox,

          // ✅ unlock vs send
          showEdit: !unlocked,
          editLabel: "Change phone",

          showSend: unlocked,
          sendLabel,

          // ✅ remove Back entirely (match email edit page)
          showBack: false,

          // ✅ always show a single exit button
          showSkip: true,
          skipLabel,

          showCancel: true,

          // ✅ key: input lock comes from unlocked mode
          phoneDisabled: !unlocked,
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