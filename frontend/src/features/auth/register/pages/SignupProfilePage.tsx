// src/features/auth/register/pages/SignupProfilePage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getApiError } from "@/shared/api/apiError";

import { useSignupFlow } from "../hooks/useSignupFlow";
import { useSignupShellUi } from "../hooks/useSignupShellUi";

import { SignupProfileStep } from "../components/forms/SignupProfileStep";
import { useSignupProfileForm } from "../forms/hooks/useSignupProfileForm";
import {
  signupProfileSchema,
  genderEnum,
  type SignupProfileFormInput,
} from "../forms/schemas/signupProfile.schema";

import { ROUTES } from "@/routes/paths";
import { signupPathFromState } from "../routes/signupPathFromState";

export default function SignupProfilePage() {
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi();
  const profileForm = useSignupProfileForm();
  const navigate = useNavigate();

  // Prefill from server, never overwrite user edits
  useEffect(() => {
    const s = flow.status;
    if (!s) return;

    const dirty = profileForm.formState.dirtyFields;

    if (s.name && !dirty.name) {
      profileForm.setValue("name", s.name, { shouldValidate: true });
    }

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
    flow.status,
    profileForm,
    profileForm.formState.dirtyFields,
  ]);

  const clearUiErrors = () => {
    shellUi.clearBanner();
    profileForm.clearErrors(); // clears server inline errors too
  };

  const onSubmit = async (values: SignupProfileFormInput) => {
    clearUiErrors();

    // client-side strict validation (keeps your existing behavior)
    const parsed = signupProfileSchema.parse(values);

    try {
      const res = await flow.updateProfile({
        name: parsed.name,
        gender: parsed.gender,
        birthDate: parsed.birthDate,
      });

      navigate(signupPathFromState(res.state));
    } catch (err) {
      const apiErr = getApiError(err);

      // ✅ inline: bad user input
      if (apiErr?.code === "INVALID_INPUT") {
        // If BE doesn't provide per-field details, put it on a "safe" field
        profileForm.setError("name", { type: "server", message: apiErr.message });
        return;
      }

      // ✅ banner: session/state/unknown/network
      shellUi.showBanner({
        kind: "error",
        code: apiErr?.code,
        message: apiErr?.message ?? "Profile update failed.",
      });

      // optional: if session is missing, you can redirect user out
      // if (apiErr?.code === "NO_SESSION") navigate(ROUTES.LOGIN, { replace: true });
    }
  };

  const onCancel = () => {
    shellUi.openCancelModal({
      reason: "user",
      title: "Cancel signup?",
      description: "Your signup progress will be discarded. You can start again anytime.",
      confirmLabel: "Cancel signup",
      cancelLabel: "Keep going",
      destructive: true,
      onConfirm: async () => {
        shellUi.clearBanner(); // optional
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupProfileStep
        // ✅ don’t use flow.error banner + toast + inline at same time
        // Use the banner we added in Shell UI + RHF inline errors
        error={undefined}
        form={profileForm}
        onSubmit={onSubmit}
        onCancel={onCancel}
        saving={Boolean(flow.loading?.updateProfile)}
        cancelling={Boolean(flow.loading?.cancel)}
      />
    </div>
  );
}
