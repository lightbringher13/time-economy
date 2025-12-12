// features/auth/pages/RegisterPage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FormProvider } from "react-hook-form";

import { ROUTES } from "@/routes/paths";
import { useAuthStore } from "@/store/useAuthStore";

import { useRegisterForm } from "../hooks/useRegisterForm";
import { useRegisterApis } from "../hooks/useRegisterApis";

import { EmailSection } from "../components/EmailSection";
import { PhoneSection } from "../components/PhoneSection";
import { PasswordSection } from "../components/PasswordSection";
import { ProfileSection } from "../components/ProfileSection";
import { SubmitSection } from "../components/SubmitSection";

export default function RegisterPage() {
  const navigate = useNavigate();
  const phase = useAuthStore((s) => s.phase);

  // redirect if already logged in
  useEffect(() => {
    if (phase === "authenticated") {
      navigate(ROUTES.DASHBOARD, { replace: true });
    }
  }, [phase, navigate]);

  const form = useRegisterForm();
  const {
    bootstrapLoading,
    error,
    emailSectionProps,
    phoneSectionProps,
    submitProps,
    onSubmit,
  } = useRegisterApis(form);

  return (
    <div style={{ maxWidth: 400, margin: "40px auto" }}>
      <h1>Register</h1>
      <p style={{ marginBottom: 16 }}>
        Create a TimeEconomy account to start tracking your expenses.
      </p>

      {bootstrapLoading && <p>Loading signup info...</p>}

      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)}>
          <EmailSection {...emailSectionProps} />
          <PasswordSection />
          <PhoneSection {...phoneSectionProps} />
          <ProfileSection />
          <SubmitSection error={error} loading={submitProps.loading} />
        </form>
      </FormProvider>

      <p style={{ marginTop: 16 }}>
        Already have an account?{" "}
        <button
          type="button"
          onClick={() => navigate(ROUTES.LOGIN)}
          style={{
            border: "none",
            background: "none",
            color: "#0070f3",
            cursor: "pointer",
            padding: 0,
            textDecoration: "underline",
          }}
        >
          Log in
        </button>
      </p>

      <p style={{ marginTop: 16 }}>
        Backend Health Check?{" "}
        <button
          type="button"
          onClick={() => navigate(ROUTES.HEALTH)}
          style={{
            border: "none",
            background: "none",
            color: "#0070f3",
            cursor: "pointer",
            padding: 0,
            textDecoration: "underline",
          }}
        >
          HealthCheck
        </button>
      </p>
    </div>
  );
}