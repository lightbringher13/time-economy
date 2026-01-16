// src/features/auth/register/pages/SignupReviewPage.tsx
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { useSignupFlow } from "../hooks/SignupFlowContext.tsx";

import { SignupReviewStep } from "../components/SignupReviewStep";
import { useSignupPasswordForm } from "../forms/hooks/useSignupPasswordForm";

export default function SignupReviewPage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();
  const passwordForm = useSignupPasswordForm();

  const onCreate = async (password: string) => {
    const s = flow.status;

    // must be complete before register
    const email = s?.email ?? null;
    const phoneNumber = s?.phoneNumber ?? null;
    const name = s?.name ?? null;
    const gender = s?.gender ?? null;
    const birthDate = s?.birthDate ?? null;

    if (!email || !phoneNumber || !name || !gender || !birthDate) {
      navigate(ROUTES.SIGNUP_PROFILE, { replace: true });
      return;
    }

    // ✅ register (cookie may be cleared by BE)
    await flow.register({
      email,
      password,
      phoneNumber,
      name,
      gender,
      birthDate,
    });

    // ✅ done should be outside the /signup layout tree
    navigate(ROUTES.SIGNUP_DONE, { replace: true });
  };

  const onBack = () => {
    // go to profile page explicitly
    navigate(ROUTES.SIGNUP_PROFILE);
  };

  const onCancel = async () => {
    // ✅ important ordering: leave /signup first so layout doesn't rerun things
    navigate(ROUTES.LOGIN, { replace: true });
    await flow.cancel();
  };

  // edit actions are navigation-only
  const onEditEmail = () => navigate(ROUTES.SIGNUP_EMAIL_EDIT, {
    state: { from: "review", startLocked: false },
  });
  const onEditPhone = () => navigate(ROUTES.SIGNUP_EMAIL_EDIT, {
    state: { from: "review", startLocked: false },
  });

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