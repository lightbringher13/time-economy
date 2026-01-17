// src/features/auth/register/pages/SignupReviewPage.tsx
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { useSignupFlow } from "../hooks/SignupFlowContext";
import { useSignupShellUi } from "../hooks/SignupShellUiContext";

import { SignupReviewStep } from "../components/forms/SignupReviewStep";
import { useSignupPasswordForm } from "../forms/hooks/useSignupPasswordForm";

export default function SignupReviewPage() {
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi();

  const navigate = useNavigate();
  const passwordForm = useSignupPasswordForm();

  const onCreate = async (password: string) => {
    const s = flow.status;

    const email = s?.email ?? null;
    const phoneNumber = s?.phoneNumber ?? null;
    const name = s?.name ?? null;
    const gender = s?.gender ?? null;
    const birthDate = s?.birthDate ?? null;

    if (!email || !phoneNumber || !name || !gender || !birthDate) {
      navigate(ROUTES.SIGNUP_PROFILE, { replace: true });
      return;
    }

    await flow.register({
      email,
      password,
      phoneNumber,
      name,
      gender,
      birthDate,
    });

    navigate(ROUTES.SIGNUP_DONE, { replace: true });
  };

  const onBack = () => {
    navigate(ROUTES.SIGNUP_PROFILE);
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
        // leave signup tree first so layout stops reacting
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
  };

  const onEditEmail = () => {
    shellUi.openConfirmModal({
      title: "Edit email?",
      description:
        "Changing your email requires verifying the new email again. Your progress will be kept, but verification will be required.",
      confirmLabel: "Continue",
      cancelLabel: "Stay here",
      onConfirm: () => {
        navigate(ROUTES.SIGNUP_EMAIL_EDIT, {
          state: { from: "review", startLocked: false },
        });
      },
    });
  };

  const onEditPhone = () => {
    shellUi.openConfirmModal({
      title: "Edit phone number?",
      description:
        "Changing your phone number requires verifying the new phone again. Your progress will be kept, but verification will be required.",
      confirmLabel: "Continue",
      cancelLabel: "Stay here",
      onConfirm: () => {
        navigate(ROUTES.SIGNUP_PHONE_EDIT, {
          state: { from: "review", startLocked: false },
        });
      },
    });
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupReviewStep
        error={flow.error ?? undefined}
        data={{
          email: flow.status?.email ?? null,
          phoneNumber: flow.status?.phoneNumber ?? null,
          name: flow.status?.name ?? null,
          gender: flow.status?.gender ?? null,
          birthDate: flow.status?.birthDate ?? null,
        }}
        passwordForm={passwordForm}
        ui={{
          title: "Create your account",
          subtitle: "Step 4 — Review",
          showCreate: true,
          showBack: true,
          showCancel: true,
          showEditEmail: true,
          showEditPhone: true,
          editEmailLabel: "Edit email",
          editPhoneLabel: "Edit phone",
          createLabel: "Create account",
          backLabel: "Back",
          cancelLabel: "Cancel",
        }}
        loading={{
          create: Boolean(flow.loading?.register),
          cancel: Boolean(flow.loading?.cancel),
          back: false,
          editEmail: false,
          editPhone: false,
        }}
        actions={{
          create: onCreate,
          back: onBack,
          cancel: onCancel,
          editEmail: onEditEmail,
          editPhone: onEditPhone,
        }}
      />
    </div>
  );
}