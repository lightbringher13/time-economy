// src/features/auth/register/pages/RegisterPage.tsx
import { useEffect, useMemo, useState, useRef } from "react";

import { useSignupFlow } from "../hooks/useSignupFlow";

import { SignupEmailStep } from "../components/SignupEmailStep";
import { SignupPhoneStep } from "../components/SignupPhoneStep";
import { SignupProfileStep } from "../components/SignupProfileStep";
import { SignupDoneStep } from "../components/SignupDoneStep";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";
import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";
import { useSignupProfileForm } from "../forms/hooks/useSignupProfileForm";

import type { SignupSessionState } from "@/features/auth/register/api/signupApi.types";
import type { SignupProfileFormValues } from "../forms/schemas/signupProfile.schema";

type LocalStep = "EMAIL" | "PHONE" | "PROFILE" | "DONE";

/**
 * Local step is ONLY for "Back" navigation UX.
 * Server state still drives correctness.
 */
function localStepFromServer(state: SignupSessionState | null): LocalStep {
  switch (state) {
    case "PROFILE_PENDING":
      return "PROFILE";
    case "COMPLETED":
      return "DONE";
    case "EMAIL_VERIFIED":
    case "PHONE_OTP_SENT":
      return "PHONE";
    case "DRAFT":
    case "EMAIL_OTP_SENT":
    default:
      return "EMAIL";
  }
}

export function RegisterPage() {
  const flow = useSignupFlow();

  // ✅ forms (from your hooks)
  const emailForm = useSignupEmailForm();
  const emailOtpForm = useSignupEmailOtpForm();
  const phoneForm = useSignupPhoneForm();
  const phoneOtpForm = useSignupPhoneOtpForm();
  const profileForm = useSignupProfileForm();

  // ----- local-only back navigation -----
  const [localStep, setLocalStep] = useState<LocalStep | null>(null);

  const serverStep = useMemo(
    () => localStepFromServer(flow.view.state),
    [flow.view.state]
  );

  // ✅ single source of truth for what to render
  const step: LocalStep = localStep ?? serverStep;

  // ----- bootstrap on mount -----
  const bootedRef = useRef(false);
  useEffect(() => {
    if (bootedRef.current) return;
    bootedRef.current = true;

    (async () => {
      try {
        await flow.bootstrap();
        await flow.refresh();
      } catch {
        // error already set by hook
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // if server reaches DONE, don’t allow local step to override
  useEffect(() => {
    if (serverStep === "DONE") setLocalStep(null);
  }, [serverStep]);

  // populate forms from server (only when empty / first load)
  useEffect(() => {
    const v = flow.view;

    if (v.email && !emailForm.getValues("email")) {
      emailForm.setValue("email", v.email, { shouldValidate: true });
    }
    if (v.phoneNumber && !phoneForm.getValues("phoneNumber")) {
      phoneForm.setValue("phoneNumber", v.phoneNumber, { shouldValidate: true });
    }
    if (v.name && !profileForm.getValues("name")) {
      profileForm.setValue("name", v.name, { shouldValidate: true });
    }
    if (v.gender && !profileForm.getValues("gender")) {
      profileForm.setValue("gender", v.gender as any, { shouldValidate: true });
    }
    if (v.birthDate && !profileForm.getValues("birthDate")) {
      profileForm.setValue("birthDate", v.birthDate, { shouldValidate: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    flow.view.email,
    flow.view.phoneNumber,
    flow.view.name,
    flow.view.gender,
    flow.view.birthDate,
  ]);

  const loading = flow.loading;

  // ----- helpers -----
  const backToEmail = () => setLocalStep("EMAIL");
  const backToPhone = () => setLocalStep("PHONE");

  const cancel = async () => {
    try {
      await flow.cancel();

      // reset local UX + forms
      setLocalStep("EMAIL");
      emailOtpForm.reset({ code: "" });
      phoneOtpForm.reset({ code: "" });
      profileForm.reset({ name: "", gender: "MALE", birthDate: "" });

      flow.setError(null);
    } catch {
      // hook error set
    }
  };

  // ----- EMAIL handlers -----
  const editEmail = async (newEmail: string) => {
    await flow.editEmail({ newEmail });

    // editing email invalidates phone/profile in your domain -> reset FE forms too
    phoneForm.reset({ phoneNumber: "" });
    phoneOtpForm.reset({ code: "" });
    profileForm.reset({ name: "", gender: "MALE", birthDate: "" });

    setLocalStep(null);
  };

  const sendEmailOtp = async () => {
    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = emailForm.getValues("email");
    await editEmail(email); // persist it first
    await flow.sendEmailOtp(); // then send
    setLocalStep(null);
  };

  const resendEmailOtp = async () => {
    await flow.resendEmailOtp();
  };

  const verifyEmailOtp = async (code: string) => {
    await flow.verifyEmailOtp(code);
    setLocalStep(null);
  };

  // ----- PHONE handlers -----
  const editPhone = async (newPhone: string) => {
    await flow.editPhone({ newPhoneNumber: newPhone });

    // editing phone invalidates profile
    profileForm.reset({ name: "", gender: "MALE", birthDate: "" });
    phoneOtpForm.reset({ code: "" });

    setLocalStep(null);
  };

  const sendPhoneOtp = async () => {
    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");
    await editPhone(phone);
    await flow.sendPhoneOtp();
    setLocalStep(null);
  };

  const resendPhoneOtp = async () => {
    await flow.resendPhoneOtp();
  };

  const verifyPhoneOtp = async (code: string) => {
    await flow.verifyPhoneOtp(code);
    setLocalStep(null);
  };

  // ----- PROFILE submit -----
  const submitProfile = async (values: SignupProfileFormValues) => {
    await flow.updateProfile({
      email: flow.view.email,             // optional
      phoneNumber: flow.view.phoneNumber, // optional
      name: values.name,
      gender: values.gender,
      birthDate: values.birthDate,        // must be "YYYY-MM-DD"
    });

    setLocalStep(null);
  };

  // ----- UI -----
  const blocked = flow.uiStep === "CANCELED" || flow.uiStep === "EXPIRED";

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      {flow.uiStep === "CANCELED" && (
        <div>
          <h2>Signup canceled</h2>
          <p style={{ marginTop: 8, color: "#666" }}>
            You canceled the signup flow. Refresh to start again.
          </p>
        </div>
      )}

      {flow.uiStep === "EXPIRED" && (
        <div>
          <h2>Signup expired</h2>
          <p style={{ marginTop: 8, color: "#666" }}>
            Your signup session expired. Refresh to start again.
          </p>
        </div>
      )}

      {!blocked && step === "EMAIL" && (
        <SignupEmailStep
          state={flow.view.state}
          emailVerified={flow.view.emailVerified}
          maskedEmail={flow.view.email}
          loading={loading}
          error={flow.error}
          emailForm={emailForm}
          otpForm={emailOtpForm}
          onSendOtp={sendEmailOtp}
          onResendOtp={resendEmailOtp}
          onVerifyOtp={verifyEmailOtp}
          onEditEmail={editEmail}
          onCancel={cancel}
        />
      )}

      {!blocked && step === "PHONE" && (
        <SignupPhoneStep
          state={flow.view.state}
          phoneVerified={flow.view.phoneVerified}
          maskedPhone={flow.view.phoneNumber}
          loading={loading}
          error={flow.error}
          phoneForm={phoneForm}
          otpForm={phoneOtpForm}
          onSendOtp={sendPhoneOtp}
          onResendOtp={resendPhoneOtp}
          onVerifyOtp={verifyPhoneOtp}
          onEditPhone={editPhone}
          onBackToEmail={backToEmail}
          onCancel={cancel}
        />
      )}

      {!blocked && step === "PROFILE" && (
        <SignupProfileStep
          state={flow.view.state}
          loading={loading}
          error={flow.error}
          form={profileForm}
          onSubmit={submitProfile}
          onBackToPhone={backToPhone}
          onCancel={cancel}
        />
      )}

      {!blocked && step === "DONE" && (
        <SignupDoneStep loading={loading} message={null} />
      )}

      {/* debug */}
      <div style={{ marginTop: 16, fontSize: 12, color: "#666" }}>
        <div>uiStep: {flow.uiStep}</div>
        <div>serverState: {flow.view.state ?? "-"}</div>
        <div>step(rendered): {step}</div>
        <button
          type="button"
          onClick={() => flow.refresh()}
          disabled={loading}
          style={{ marginTop: 8, padding: "6px 10px" }}
        >
          Refresh
        </button>
      </div>
    </div>
  );
}