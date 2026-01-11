// src/features/auth/register/components/SignupProfileStep.tsx
import type { UseFormReturn } from "react-hook-form";

import type {
  SignupProfileFormValues,
  SignupGender,
} from "../forms/schemas/signupProfile.schema";

type ProfileUi = {
  title?: string; // default: "Create your account"
  subtitle?: string; // default: "Step 3 — Profile"

  // what to show
  showSubmit?: boolean; // default: true
  showBack?: boolean;
  showCancel?: boolean;
  showTip?: boolean; // default: true

  // labels
  submitLabel?: string; // default: "Continue"
  backLabel?: string; // default: "Back"
  cancelLabel?: string; // default: "Cancel"

  tipText?: string; // default: "Tip: You can change phone by going back, or using “Edit phone” in the previous step."
};

type ProfileLoading = {
  save?: boolean;
  cancel?: boolean;
};

type ProfileActions = {
  submit?: (values: SignupProfileFormValues) => void | Promise<void>;
  back?: () => void | Promise<void>;
  cancel?: () => void | Promise<void>;
};

interface Props {
  error?: string | null;

  form: UseFormReturn<SignupProfileFormValues>;

  ui?: ProfileUi;
  loading?: ProfileLoading;
  actions: ProfileActions;
}

const genderOptions: Array<{ value: SignupGender; label: string }> = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
  { value: "OTHER", label: "Other" },
];

export function SignupProfileStep({ error, form, ui, loading, actions }: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = form;

  const isSaving = Boolean(loading?.save);
  const isCancelling = Boolean(loading?.cancel);
  const busy = isSaving || isCancelling;

  const title = ui?.title ?? "Create your account";
  const subtitle = ui?.subtitle ?? "Step 3 — Profile";

  const showSubmit = ui?.showSubmit ?? true;
  const showBack = Boolean(ui?.showBack);
  const showCancel = Boolean(ui?.showCancel);
  const showTip = ui?.showTip ?? true;

  const submitLabel = ui?.submitLabel ?? "Continue";
  const backLabel = ui?.backLabel ?? "Back";
  const cancelLabel = ui?.cancelLabel ?? "Cancel";

  const tipText =
    ui?.tipText ??
    "Tip: You can change phone by going back, or using “Edit phone” in the previous step.";

  const onSubmit = handleSubmit(async (values) => {
    await actions.submit?.(values);
  });

  return (
    <form onSubmit={onSubmit} noValidate>
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
            {errors.gender.message}
          </div>
        )}
      </div>

      {/* Birth date */}
      <div style={{ marginTop: 16 }}>
        <label
          htmlFor="birthDate"
          style={{ display: "block", marginBottom: 4 }}
        >
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
            {errors.birthDate.message}
          </div>
        )}
      </div>

      {/* Actions */}
      <div style={{ display: "flex", gap: 8, marginTop: 16, flexWrap: "wrap" }}>
        {showSubmit && (
          <button
            type="submit"
            disabled={busy || !actions.submit}
            style={{ padding: "8px 14px" }}
          >
            {isSaving ? "Saving..." : submitLabel}
          </button>
        )}

        {showBack && (
          <button
            type="button"
            onClick={actions.back}
            disabled={busy || !actions.back}
            style={{ padding: "8px 14px" }}
          >
            {backLabel}
          </button>
        )}

        {showCancel && (
          <button
            type="button"
            onClick={actions.cancel}
            disabled={busy || !actions.cancel}
            style={{ padding: "8px 14px" }}
          >
            {isCancelling ? "Cancelling..." : cancelLabel}
          </button>
        )}
      </div>

      {showTip && (
        <div style={{ marginTop: 10, fontSize: 12, color: "#666" }}>
          {tipText}
        </div>
      )}
    </form>
  );
}