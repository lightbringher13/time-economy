// src/features/auth/register/pages/SignupReviewPage.tsx
import { useEffect, useMemo, useRef } from "react";
import { useNavigate } from "react-router-dom";

import { useSignupFlow } from "../hooks/useSignupFlow";
import { signupPathFromState } from "../routes/signupRouteMap";

import { SignupReviewStep } from "../components/SignupReviewStep";
import { useSignupPasswordForm } from "../forms/hooks/useSignupPasswordForm";

export default function SignupReviewPage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();

  const passwordForm = useSignupPasswordForm();

  // ---- bootstrap once (ensures cookie/session exists) ----
  const bootedRef = useRef(false);
  useEffect(() => {
    if (bootedRef.current) return;
    bootedRef.current = true;
    void flow.bootstrap();
  }, [flow.bootstrap]);

  // ---- route guard: if server state says not REVIEW, redirect ----
  const expectedPath = useMemo(() => {
    return signupPathFromState(flow.state);
  }, [flow.state]);

  useEffect(() => {
    if (!flow.state) return;

    if (expectedPath !== "/signup/review") {
      navigate(expectedPath, { replace: true });
    }
  }, [expectedPath, flow.state, navigate]);

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

    // ✅ register clears cookie => do NOT rely on status after this
    navigate("/signup/done", { replace: true });
  };

  const onBack = () => {
    navigate("/signup/profile");
  };

  const onCancel = async () => {
    await flow.cancel();
    navigate("/signup/email", { replace: true });
  };

  // ✅ per-action flags (no global loading)
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
          createLabel: "Create account",
          backLabel: "Back",
          cancelLabel: "Cancel",
        }}
        loading={{
          create: isCreating,
          cancel: isCancelling,
          back: false,
        }}
        actions={{
          create: onCreate,
          back: onBack,
          cancel: onCancel,
        }}
      />
    </div>
  );
}