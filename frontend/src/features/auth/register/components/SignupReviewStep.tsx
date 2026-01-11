// src/features/auth/register/components/SignupReviewStep.tsx
import type { UseFormReturn } from "react-hook-form";
import type { SignupPasswordFormValues } from "../forms/schemas/signupPassword.schema";

type ReviewData = {
  email: string | null;
  phoneNumber: string | null;
  name: string | null;
  gender: string | null;
  birthDate: string | null;
};

type ReviewUi = {
  title?: string;       // default: "Create your account"
  subtitle?: string;    // default: "Step 4 — Review"

  showCreate?: boolean; // default: true
  showBack?: boolean;   // default: true
  showCancel?: boolean; // default: true

  createLabel?: string; // default: "Create account"
  backLabel?: string;   // default: "Back"
  cancelLabel?: string; // default: "Cancel"
};

type ReviewLoading = {
  create?: boolean;
  back?: boolean;
  cancel?: boolean;
};

type ReviewActions = {
  create?: (password: string) => void | Promise<void>;
  back?: () => void | Promise<void>;
  cancel?: () => void | Promise<void>;
};

interface Props {
  error?: string | null;

  data: ReviewData;

  passwordForm: UseFormReturn<SignupPasswordFormValues>;

  ui?: ReviewUi;
  loading?: ReviewLoading;
  actions: ReviewActions;
}

export function SignupReviewStep({
  error,
  data,
  passwordForm,
  ui,
  loading,
  actions,
}: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = passwordForm;

  const isCreating = Boolean(loading?.create);
  const isGoingBack = Boolean(loading?.back);
  const isCancelling = Boolean(loading?.cancel);

  const busy = isCreating || isGoingBack || isCancelling;

  const title = ui?.title ?? "Create your account";
  const subtitle = ui?.subtitle ?? "Step 4 — Review";

  const showCreate = ui?.showCreate ?? true;
  const showBack = ui?.showBack ?? true;
  const showCancel = ui?.showCancel ?? true;

  const createLabel = ui?.createLabel ?? "Create account";
  const backLabel = ui?.backLabel ?? "Back";
  const cancelLabel = ui?.cancelLabel ?? "Cancel";

  const submit = handleSubmit(async (values) => {
    await actions.create?.(values.password);
  });

  return (
    <form onSubmit={submit} noValidate>
      <h2>{title}</h2>
      <h3 style={{ marginTop: 8 }}>{subtitle}</h3>

      {error && (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>{error}</div>
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
        <Row label="Email" value={data.email} />
        <Row label="Phone" value={data.phoneNumber} />
        <Row label="Name" value={data.name} />
        <Row label="Gender" value={data.gender} />
        <Row label="Birth date" value={data.birthDate} />
      </div>

      {/* Password */}
      <div style={{ marginTop: 16 }}>
        <label htmlFor="password" style={{ display: "block", marginBottom: 4 }}>
          Password
        </label>
        <input
          id="password"
          type="password"
          autoComplete="new-password"
          disabled={busy}
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
          disabled={busy}
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
        {showCreate && (
          <button
            type="submit"
            disabled={busy || !actions.create}
            style={{ padding: "8px 14px" }}
          >
            {isCreating ? "Creating..." : createLabel}
          </button>
        )}

        {showBack && (
          <button
            type="button"
            onClick={actions.back}
            disabled={busy || !actions.back}
            style={{ padding: "8px 14px" }}
          >
            {isGoingBack ? "Going back..." : backLabel}
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