// src/features/auth/register/pages/SignupProfilePage.tsx
import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { useSignupFlow } from "../hooks/useSignupFlow";
import { SignupProfileStep } from "../components/SignupProfileStep";

import { useSignupProfileForm } from "../forms/hooks/useSignupProfileForm";
import type { SignupProfileFormValues } from "../forms/schemas/signupProfile.schema";

import { signupPathFromState } from "../routes/signupRouteMap";
import type { SignupSessionState } from "../api/signupApi.types";

const PROFILE_PAGE_ALLOWED: SignupSessionState[] = ["PROFILE_PENDING","PROFILE_READY"];

export default function SignupProfilePage() {
  const flow = useSignupFlow();
  const profileForm = useSignupProfileForm();

  const navigate = useNavigate();
  const location = useLocation();

  // ---- bootstrap once ----
  const bootedRef = useRef(false);
  useEffect(() => {
    if (bootedRef.current) return;
    bootedRef.current = true;
    void flow.bootstrap();
  }, [flow.bootstrap]);

  // ---- route guard ----
  useEffect(() => {
    if (!flow.state) return;

    if (PROFILE_PAGE_ALLOWED.includes(flow.state)) return;

    const path = signupPathFromState(flow.state);
    if (path !== location.pathname) {
      navigate(path, { replace: true });
    }
  }, [flow.state, navigate, location.pathname]);

  // ---- prefill from server once ----
  useEffect(() => {
    const s = flow.status;
    if (!s) return;

    if (s.name && !profileForm.getValues("name")) {
      profileForm.setValue("name", s.name, { shouldValidate: true });
    }
    if (s.gender && !profileForm.getValues("gender")) {
      profileForm.setValue("gender", s.gender as any, { shouldValidate: true });
    }
    if (s.birthDate && !profileForm.getValues("birthDate")) {
      profileForm.setValue("birthDate", s.birthDate, { shouldValidate: true });
    }
  }, [flow.status?.name, flow.status?.gender, flow.status?.birthDate, profileForm]);

  // ---- handlers ----
  const onSubmit = async (values: SignupProfileFormValues) => {
    await flow.updateProfile({
      email: flow.status?.email ?? null,
      phoneNumber: flow.status?.phoneNumber ?? null,
      name: values.name,
      gender: values.gender,
      birthDate: values.birthDate,
    });

    // optional (recommended): navigate immediately for better UX
    navigate("/signup/review", { replace: true });
  };

  const onBackToPhone = () => {
    navigate("/signup/edit/phone");
  };

  const onCancel = async () => {
    await flow.cancel();
    // since cancel may remove cache / state becomes null, redirect explicitly
    navigate("/signup/email", { replace: true });
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupProfileStep
        error={flow.error ?? undefined}
        form={profileForm}
        ui={{
          title: "Create your account",
          subtitle: "Step 3 — Profile",
          showSubmit: true,
          showBack: true,
          showCancel: true,
          showTip: true,
          submitLabel: "Continue",
          backLabel: "Back",
          cancelLabel: "Cancel",
        }}
        loading={{
          save: Boolean(flow.loading?.updateProfile),
          cancel: Boolean(flow.loading?.cancel),
        }}
        actions={{
          submit: onSubmit,
          back: onBackToPhone,
          cancel: onCancel,
        }}
      />
    </div>
  );
}