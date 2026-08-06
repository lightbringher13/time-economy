// src/features/auth/register/pages/SignupReviewPage.tsx
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { useSignupFlow } from "../hooks/useSignupFlow";
import { useSignupShellUi } from "../hooks/useSignupShellUi";

import { SignupReviewStep } from "../components/forms/SignupReviewStep";
import { useSignupPasswordForm } from "../forms/hooks/useSignupPasswordForm";

import { getApiError } from "@/shared/api/apiError";

export default function SignupReviewPage() {
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi();
  const navigate = useNavigate();
  const passwordForm = useSignupPasswordForm();

  const clearUiErrors = () => {
    passwordForm.clearErrors();
    shellUi.clearBanner();
  };

  const onCreate = async (password: string) => {
    clearUiErrors();

    try {
      await flow.register({ password }); // throws on non-2xx
      shellUi.clearBanner();
      navigate(ROUTES.SIGNUP_DONE, { replace: true });
    } catch (err) {
      const apiErr = getApiError(err);

      // ✅ inline (field-level)
      if (apiErr?.code === "INVALID_INPUT") {
        passwordForm.setError("password", { type: "server", message: apiErr.message });
        return;
      }

      // ✅ banner (session/state/conflict/network/etc.)
      shellUi.showBanner({
        kind: "error",
        code: apiErr?.code,
        message: apiErr?.message ?? "Registration failed. Please try again.",
      });

      // ✅ optional big-co routing: if session is dead, exit signup
      if (
        apiErr?.code === "NO_SESSION" ||
        apiErr?.code === "SESSION_EXPIRED_OR_NOT_FOUND"
      ) {
        navigate(ROUTES.LOGIN, { replace: true });
      }
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
        shellUi.clearBanner(); // prevent banner leaking to login
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
  };

  const onEditEmail = () => {
    clearUiErrors();
    shellUi.openConfirmModal({
      title: "Edit email?",
      description:
        "Changing your email requires verifying the new email again. Your progress will be kept, but verification will be required.",
      confirmLabel: "Continue",
      cancelLabel: "Stay here",
      onConfirm: () => navigate(ROUTES.SIGNUP_EMAIL),
    });
  };

  const onEditPhone = () => {
    clearUiErrors();
    shellUi.openConfirmModal({
      title: "Edit phone number?",
      description:
        "Changing your phone number requires verifying the new phone again. Your progress will be kept, but verification will be required.",
      confirmLabel: "Continue",
      cancelLabel: "Stay here",
      onConfirm: () => navigate(ROUTES.SIGNUP_PHONE),
    });
  };

  const onEditProfile = () => {
    clearUiErrors();
    shellUi.openConfirmModal({
      title: "Edit profile?",
      description: "You can update your name, gender, and birth date. Your progress will be kept.",
      confirmLabel: "Continue",
      cancelLabel: "Stay here",
      onConfirm: () => navigate(ROUTES.SIGNUP_PROFILE),
    });
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupReviewStep
        // ✅ banner is shown globally by ShellUiContext, so don't also show step error
        error={undefined}
        data={{
          email: flow.status?.email ?? null,
          phoneNumber: flow.status?.phoneNumber ?? null,
          name: flow.status?.name ?? null,
          gender: flow.status?.gender ?? null,
          birthDate: flow.status?.birthDate ?? null,
        }}
        passwordForm={passwordForm}
        loadingCreate={Boolean(flow.loading?.register)}
        loadingCancel={Boolean(flow.loading?.cancel)}
        onCreate={onCreate}
        onCancel={onCancel}
        onEditEmail={onEditEmail}
        onEditPhone={onEditPhone}
        onEditProfile={onEditProfile}
      />
    </div>
  );
}
