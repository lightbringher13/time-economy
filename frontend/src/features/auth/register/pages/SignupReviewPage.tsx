// src/features/auth/register/pages/SignupReviewPage.tsx
import { useNavigate } from "react-router-dom";

import { useSignupFlow } from "../hooks/SignupFlowContext.tsx";

import { SignupReviewStep } from "../components/SignupReviewStep";
import { useSignupPasswordForm } from "../forms/hooks/useSignupPasswordForm";
import { ROUTES } from "@/routes/paths.ts";

export default function SignupReviewPage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();

  const passwordForm = useSignupPasswordForm();

  // ---- actions ----
  const onCreate = async (password: string) => {
    const s = flow.status;

    const email = s?.email ?? null;
    const phoneNumber = s?.phoneNumber ?? null;
    const name = s?.name ?? null;
    const gender = s?.gender ?? null;
    const birthDate = s?.birthDate ?? null;

    if (!email || !phoneNumber || !name || !gender || !birthDate) {
      navigate("/signup/profile", { replace: true });
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

    navigate("/done", { replace: true });

    void flow.clearSignupCache();
  };

  const onBack = () => {
    navigate("/signup/profile");
  };

  const onCancel = async () => {
    navigate(ROUTES.LOGIN, {replace: true});
    await flow.cancel();
    
  };

  const onEditEmail = () => {
    navigate("/signup/edit/email");
  };

  const onEditPhone = () => {
    navigate("/signup/edit/phone");
  };

  const isCreating = flow.mutations.registerMu.isPending;
  const isCancelling = flow.mutations.cancelMu.isPending;

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
          create: isCreating,
          cancel: isCancelling,
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