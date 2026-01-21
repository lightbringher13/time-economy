// src/features/auth/register/components/forms/SignupProfileStep.tsx
import type { UseFormReturn } from "react-hook-form";
import type {
  SignupProfileFormInput,
  SignupGender,
} from "../../forms/schemas/signupProfile.schema";

type Props = {
  title?: string;
  subtitle?: string;

  error?: string | null;

  form: UseFormReturn<SignupProfileFormInput>;

  onSubmit: (values: SignupProfileFormInput) => void | Promise<void>;
  onCancel?: () => void | Promise<void>;

  saving?: boolean;
  cancelling?: boolean;

  submitLabel?: string;
  cancelLabel?: string;
};

const genderOptions: Array<{ value: SignupGender; label: string }> = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
];

export function SignupProfileStep({
  title = "Create your account",
  subtitle = "Step 3 — Profile",

  error,

  form,

  onSubmit,
  onCancel,

  saving = false,
  cancelling = false,

  submitLabel = "Continue",
  cancelLabel = "Cancel",
}: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form;

  const busy = saving || cancelling;

  const submit = handleSubmit(async (values) => {
    await onSubmit(values);
  });

  return (
    <form onSubmit={submit} noValidate>
      <h2>{title}</h2>
      <h3 style={{ marginTop: 8 }}>{subtitle}</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>{error}</div>
      )}

      {/* Name */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="name" style={{ display: "block", marginBottom: 4 }}>
          Name
        </label>
        <input
          id="name"
          autoComplete="name"
          disabled={busy}
          {...register("name")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.name?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {String(errors.name.message)}
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
          disabled={busy}
          {...register("gender")}
          style={{ width: "100%", padding: "8px" }}
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
            {String(errors.gender.message)}
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
          disabled={busy}
          {...register("birthDate")}
          style={{ width: "100%", padding: 8 }}
        />
        {errors.birthDate?.message && (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {String(errors.birthDate.message)}
          </div>
        )}
      </div>

      {/* Actions */}
      <div style={{ display: "flex", gap: 8, marginTop: 16, flexWrap: "wrap" }}>
        <button type="submit" disabled={busy} style={{ padding: "8px 14px" }}>
          {saving ? "Saving..." : submitLabel}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            style={{ padding: "8px 14px" }}
          >
            {cancelling ? "Cancelling..." : cancelLabel}
          </button>
        )}
      </div>
    </form>
  );
}