// src/features/auth/register/components/SignupProfileStep.tsx
import type { UseFormReturn } from "react-hook-form";

import type { SignupProfileFormValues, SignupGender } from "../forms/schemas/signupProfile.schema";
import type {SignupSessionState} from "@/features/auth/register/api/signupApi.types"

interface Props {
  state: SignupSessionState | null;
  loading: boolean;
  error?: string | null;

  form: UseFormReturn<SignupProfileFormValues>;

  onSubmit: (values: SignupProfileFormValues) => void | Promise<void>;

  onBackToPhone?: () => void | Promise<void>; // usually just FE back, or editPhone usecase
  onCancel?: () => void | Promise<void>;
}

const genderOptions: Array<{ value: SignupGender; label: string }> = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
  { value: "OTHER", label: "Other" },
];

export function SignupProfileStep({
  state,
  loading,
  error,
  form,
  onSubmit,
  onBackToPhone,
  onCancel,
}: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form;

  const canUseProfile = state === "PROFILE_PENDING";

  if (!canUseProfile) {
    return (
      <div>
        <h2>Create your account</h2>
        <p style={{ marginTop: 8, color: "#666" }}>
          Please verify email and phone first.
        </p>

        {onBackToPhone && (
          <button
            type="button"
            onClick={onBackToPhone}
            style={{ marginTop: 12, padding: "8px 14px" }}
            disabled={loading}
          >
            Back
          </button>
        )}
      </div>
    );
  }

  const submitHandler = handleSubmit(async (values) => {
    await onSubmit(values);
  });

  return (
    <form onSubmit={submitHandler} noValidate>
      <h2>Create your account</h2>
      <h3 style={{ marginTop: 8 }}>Step 3 — Profile</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>
          {error}
        </div>
      )}

      {/* Name */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="name" style={{ display: "block", marginBottom: 4 }}>
          Name
        </label>
        <input
          id="name"
          autoComplete="name"
          disabled={loading}
          {...register("name")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.name?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.name.message}
          </div>
        )}
      </div>

      {/* Gender */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="gender" style={{ display: "block", marginBottom: 4 }}>
          Gender
        </label>
        <select
          id="gender"
          disabled={loading}
          {...register("gender")}
          style={{ width: "100%", padding: 8 }}
          defaultValue=""
        >
          <option value="" disabled>
            Select...
          </option>
          {genderOptions.map((g) => (
            <option key={g.value} value={g.value}>
              {g.label}
            </option>
          ))}
        </select>
        {errors.gender?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.gender.message}
          </div>
        )}
      </div>

      {/* Birth date */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="birthDate" style={{ display: "block", marginBottom: 4 }}>
          Birth date
        </label>
        <input
          id="birthDate"
          type="date"
          disabled={loading}
          {...register("birthDate")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.birthDate?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {errors.birthDate.message}
          </div>
        )}
      </div>

      {/* Actions */}
      <div style={{ display: "flex", gap: 8, marginTop: 16, flexWrap: "wrap" }}>
        <button type="submit" disabled={loading} style={{ padding: "8px 14px" }}>
          {loading ? "Saving..." : "Continue"}
        </button>

        {onBackToPhone && (
          <button
            type="button"
            onClick={onBackToPhone}
            disabled={loading}
            style={{ padding: "8px 14px" }}
          >
            Back
          </button>
        )}

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            style={{ padding: "8px 14px" }}
          >
            Cancel
          </button>
        )}
      </div>

      <div style={{ marginTop: 10, fontSize: 12, color: "#666" }}>
        Tip: You can change phone by going back, or using “Edit phone” in the previous step.
      </div>
    </form>
  );
}