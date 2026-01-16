// src/features/auth/register/pages/SignupProfilePage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { useSignupFlow } from "../hooks/SignupFlowContext";
import { SignupProfileStep } from "../components/SignupProfileStep";

import { useSignupProfileForm } from "../forms/hooks/useSignupProfileForm";
import {
  signupProfileSchema,
  genderEnum,
  type SignupProfileFormInput,
} from "../forms/schemas/signupProfile.schema";

import { ROUTES } from "@/routes/paths";

export default function SignupProfilePage() {
  const flow = useSignupFlow();
  const profileForm = useSignupProfileForm();
  const navigate = useNavigate();

  // ✅ Prefill from server, never overwrite user edits
  useEffect(() => {
    const s = flow.status;
    if (!s) return;

    const dirty = profileForm.formState.dirtyFields;

    if (s.name && !dirty.name) {
      profileForm.setValue("name", s.name, { shouldValidate: true });
    }

    // ✅ server gender should be strict enum only
    if (s.gender && !dirty.gender) {
      const parsed = genderEnum.safeParse(s.gender);
      if (parsed.success) {
        profileForm.setValue("gender", parsed.data, { shouldValidate: true });
      }
    }

    if (s.birthDate && !dirty.birthDate) {
      profileForm.setValue("birthDate", s.birthDate, { shouldValidate: true });
    }
  }, [
    flow.status?.name,
    flow.status?.gender,
    flow.status?.birthDate,
    profileForm,
    profileForm.formState.dirtyFields,
  ]);

  // ✅ IMPORTANT: Submit receives INPUT type (gender may be "")
  const onSubmit = async (values: SignupProfileFormInput) => {
    // ✅ Convert UI input -> validated output (gender becomes "MALE" | "FEMALE")
    const parsed = signupProfileSchema.parse(values);

    await flow.updateProfile({
      name: parsed.name,
      gender: parsed.gender,
      birthDate: parsed.birthDate,
    });

    navigate(ROUTES.SIGNUP_REVIEW, { replace: true });
  };

  const onBack = () => {
    navigate(ROUTES.SIGNUP_PHONE_EDIT);
  };

  const onCancel = async () => {
    navigate(ROUTES.LOGIN, { replace: true });
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
          back: onBack,
          cancel: onCancel,
        }}
      />
    </div>
  );
}