// src/features/auth/register/pages/SignupProfilePage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { useSignupFlow } from "../hooks/SignupFlowContext.tsx";
import { SignupProfileStep } from "../components/SignupProfileStep";

import { useSignupProfileForm } from "../forms/hooks/useSignupProfileForm";
import type { SignupProfileFormValues } from "../forms/schemas/signupProfile.schema";
import { ROUTES } from "@/routes/paths.ts";

export default function SignupProfilePage() {
  const flow = useSignupFlow();
  const profileForm = useSignupProfileForm();

  const navigate = useNavigate();

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

    navigate("/signup/review");
  };

  const onBackToPhone = () => {
    navigate("/signup/edit/phone");
  };

  const onCancel = async () => {
    navigate(ROUTES.LOGIN,{replace: true});
    await flow.cancel();
    
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