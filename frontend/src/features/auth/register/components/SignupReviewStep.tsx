// src/features/auth/register/components/SignupReviewStep.tsx
import type { UseFormReturn } from "react-hook-form";
import type { SignupSessionState } from "@/features/auth/register/api/signupApi.types";
import type { SignupPasswordFormValues } from "../forms/schemas/signupPassword.schema";

interface Props {
  state: SignupSessionState | null;
  loading: boolean;
  error?: string | null;

  // data to display
  email: string | null;
  phoneNumber: string | null;
  name: string | null;
  gender: string | null;
  birthDate: string | null;

  // password form
  passwordForm: UseFormReturn<SignupPasswordFormValues>;

  // actions
  onCreate: (password: string) => void | Promise<void>;
  onBack: () => void | Promise<void>;
  onCancel: () => void | Promise<void>;
}

export function SignupReviewStep({
  state,
  loading,
  error,
  email,
  phoneNumber,
  name,
  gender,
  birthDate,
  passwordForm,
  onCreate,
  onBack,
  onCancel,
}: Props) {
  const canUseReview = state === "PROFILE_READY";

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = passwordForm;

  if (!canUseReview) {
    return (
      <div>
        <h2>Create your account</h2>
        <p style={{ marginTop: 8, color: "#666" }}>
          Please complete your profile first.
        </p>
        <button
          type="button"
          onClick={onBack}
          disabled={loading}
          style={{ marginTop: 12, padding: "8px 14px" }}
        >
          Back
        </button>
      </div>
    );
  }

  const submit = handleSubmit(async (values) => {
    await onCreate(values.password);
  });

  return (
    <form onSubmit={submit} noValidate>
      <h2>Create your account</h2>
      <h3 style={{ marginTop: 8 }}>Step 4 — Review</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>
          {error}
        </div>
      )}

      <div
        style={{
          marginTop: 16,
          border: "1px solid #ddd",
          borderRadius: 8,
          padding: 12,
          background: "#fafafa",
        }}
      >
        <Row label="Email" value={email} />
        <Row label="Phone" value={phoneNumber} />
        <Row label="Name" value={name} />
        <Row label="Gender" value={gender} />
        <Row label="Birth date" value={birthDate} />
      </div>

      {/* ✅ Password */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="password" style={{ display: "block", marginBottom: 4 }}>
          Password
        </label>
        <input
          id="password"
          type="password"
          autoComplete="new-password"
          disabled={loading}
          {...register("password")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.password?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.password.message}
          </div>
        )}
      </div>

      <div style={{ marginTop: 12 }}>
        <label
          htmlFor="passwordConfirm"
          style={{ display: "block", marginBottom: 4 }}
        >
          Confirm password
        </label>
        <input
          id="passwordConfirm"
          type="password"
          autoComplete="new-password"
          disabled={loading}
          {...register("passwordConfirm")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.passwordConfirm?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.passwordConfirm.message}
          </div>
        )}
      </div>

      <p style={{ marginTop: 12, fontSize: 12, color: "#666" }}>
        Please confirm the information above. You can go back to edit your profile.
      </p>

      <div style={{ display: "flex", gap: 8, marginTop: 16, flexWrap: "wrap" }}>
        <button type="submit" disabled={loading} style={{ padding: "8px 14px" }}>
          {loading ? "Creating..." : "Create account"}
        </button>

        <button
          type="button"
          onClick={onBack}
          disabled={loading}
          style={{ padding: "8px 14px" }}
        >
          Back
        </button>

        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          style={{ padding: "8px 14px" }}
        >
          Cancel
        </button>
      </div>
    </form>
  );
}

function Row({ label, value }: { label: string; value: string | null }) {
  return (
    <div style={{ display: "flex", gap: 10, padding: "6px 0" }}>
      <div style={{ width: 90, color: "#666", fontSize: 13 }}>{label}</div>
      <div style={{ fontSize: 13, color: "#111" }}>{value ?? "-"}</div>
    </div>
  );
}