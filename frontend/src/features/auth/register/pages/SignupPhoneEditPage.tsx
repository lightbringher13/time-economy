// src/features/auth/register/pages/SignupPhoneEditPage.tsx
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";

import { SignupPhoneStep } from "../components/forms/SignupPhoneStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";
import { useSignupShellUi } from "../hooks/SignupShellUiContext"; // ✅ UPDATED

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
  const shellUi = useSignupShellUi(); // ✅ UPDATED
  const navigate = useNavigate();
  const location = useLocation();

  const phoneForm = useSignupPhoneForm();
  const otpForm = useSignupPhoneOtpForm();

  const state = flow.view.state;

  // where did user come from?
  const from = (location.state as any)?.from as "review" | "profile" | undefined;

  // From review: start unlocked. From profile: start locked.
  const startUnlocked = from === "review";
  const [unlocked, setUnlocked] = useState<boolean>(startUnlocked);

  // committed-to-change: once true, "Back" and "Continue without changes" never appear again
  const [committedToChange, setCommittedToChange] = useState<boolean>(startUnlocked);

  // exit target: review -> review, profile/default -> profile
  const exitTarget = useMemo(() => {
    if (from === "review") return ROUTES.SIGNUP_REVIEW;
    return ROUTES.SIGNUP_PROFILE;
  }, [from]);

  const skipLabel = "Continue without changes";
  const backLabel = "Back";

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

  // ✅ UPDATED: cancel uses shell modal instead of immediate navigation/cancel
  const onCancel = () => {
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

  const onContinueWithoutChange = () => {
    navigate(exitTarget, { replace: true });
  };

  const onBackToEmail = () => {
    navigate(ROUTES.SIGNUP_EMAIL_EDIT, { replace: true, state: { from: "phone" } });
  };

  const onUnlockCommitted = async () => {
    setCommittedToChange(true); // hide skip/back forever after this
    setUnlocked(true);
    otpForm.reset({ code: "" });
  };

  const onSendCommitted = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");
    await flow.sendPhoneOtp(phone);

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

  // ✅ show Back + Skip only when coming from profile and user has not committed
  const canExitWithoutChange = from === "profile" && !committedToChange;

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

          // unlock vs send
          showEdit: !unlocked,
          editLabel: "Change phone",

          showSend: unlocked,
          sendLabel,

          // Back + Skip only in "profile + not committed" case
          showBack: canExitWithoutChange,
          backLabel,

          showSkip: canExitWithoutChange,
          skipLabel,

          showCancel: true,

          // input lock
          phoneDisabled: !unlocked,
        }}
        loading={{
          send: Boolean(flow.loading?.sendOtp),
          verify: Boolean(flow.loading?.verifyOtp),
          edit: false,
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          edit: onUnlockCommitted,
          send: onSendCommitted,
          verify: onVerify,

          back: canExitWithoutChange ? onBackToEmail : undefined,
          skip: canExitWithoutChange ? onContinueWithoutChange : undefined,

          cancel: onCancel, // ✅ UPDATED
        }}
      />
    </div>
  );
}