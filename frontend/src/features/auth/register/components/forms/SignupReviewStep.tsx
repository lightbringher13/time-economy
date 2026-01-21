// src/features/auth/register/components/forms/SignupReviewStep.tsx
import type { UseFormReturn } from "react-hook-form";
import type { SignupPasswordFormValues } from "../../forms/schemas/signupPassword.schema";

type ReviewData = {
  email: string | null;
  phoneNumber: string | null;
  name: string | null;
  gender: string | null;
  birthDate: string | null;
};

type Props = {
  error?: string | null;
  data: ReviewData;

  passwordForm: UseFormReturn<SignupPasswordFormValues>;

  loadingCreate?: boolean;
  loadingCancel?: boolean;

  onCreate: (password: string) => void | Promise<void>;
  onCancel: () => void | Promise<void>;

  onEditEmail?: () => void | Promise<void>;
  onEditPhone?: () => void | Promise<void>;
  onEditProfile?: () => void | Promise<void>;
};

export function SignupReviewStep({
  error,
  data,
  passwordForm,
  loadingCreate = false,
  loadingCancel = false,
  onCreate,
  onCancel,
  onEditEmail,
  onEditPhone,
  onEditProfile,
}: Props) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = passwordForm;

  const busy = loadingCreate || loadingCancel;

  const submit = handleSubmit(async (values) => {
    await onCreate(values.password);
  });

  return (
    <form onSubmit={submit} noValidate>
      <h2>Create your account</h2>
      <h3 style={{ marginTop: 8 }}>Step 4 — Review</h3>

      {error ? (
        <div style={{ marginTop: 12, color: "red", fontSize: 14 }}>{error}</div>
      ) : null}

      <div
        style={{
          marginTop: 16,
          border: "1px solid #ddd",
          borderRadius: 8,
          padding: 12,
          background: "#fafafa",
        }}
      >
        <Row label="Email" value={data.email} onEdit={onEditEmail} editLabel="Edit email" disabled={busy} />
        <Row label="Phone" value={data.phoneNumber} onEdit={onEditPhone} editLabel="Edit phone" disabled={busy} />

        <RowGroup title="Profile" onEdit={onEditProfile} editLabel="Edit profile" disabled={busy}>
          <Row label="Name" value={data.name} />
          <Row label="Gender" value={data.gender} />
          <Row label="Birth date" value={data.birthDate} />
        </RowGroup>
      </div>

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
        {errors.password?.message ? (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>{String(errors.password.message)}</div>
        ) : null}
      </div>

      <div style={{ marginTop: 12 }}>
        <label htmlFor="passwordConfirm" style={{ display: "block", marginBottom: 4 }}>
          Confirm password
        </label>
        <input
          id="passwordConfirm"
          type="password"
          autoComplete="new-password"
          disabled={busy}
          {...register("passwordConfirm")}
          style={{ width: "100%", padding: "8px" }}
        />
        {errors.passwordConfirm?.message ? (
          <div style={{ marginTop: 4, color: "red", fontSize: 12 }}>
            {String(errors.passwordConfirm.message)}
          </div>
        ) : null}
      </div>

      <p style={{ marginTop: 12, fontSize: 12, color: "#666" }}>
        Please confirm the information above before creating your account.
      </p>

      <div style={{ display: "flex", gap: 8, marginTop: 16, flexWrap: "wrap" }}>
        <button type="submit" disabled={busy} style={{ padding: "8px 14px" }}>
          {loadingCreate ? "Creating..." : "Create account"}
        </button>

        <button type="button" onClick={onCancel} disabled={busy} style={{ padding: "8px 14px" }}>
          {loadingCancel ? "Cancelling..." : "Cancel"}
        </button>
      </div>
    </form>
  );
}

function Row({
  label,
  value,
  onEdit,
  editLabel = "Edit",
  disabled = false,
}: {
  label: string;
  value: string | null;
  onEdit?: () => void | Promise<void>;
  editLabel?: string;
  disabled?: boolean;
}) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "6px 0" }}>
      <div style={{ width: 90, color: "#666", fontSize: 13 }}>{label}</div>
      <div style={{ flex: 1, fontSize: 13, color: "#111" }}>{value ?? "-"}</div>
      {onEdit ? (
        <button type="button" onClick={onEdit} disabled={disabled} style={{ padding: "6px 10px", fontSize: 12 }}>
          {editLabel}
        </button>
      ) : null}
    </div>
  );
}

function RowGroup({
  title,
  children,
  onEdit,
  editLabel = "Edit",
  disabled = false,
}: {
  title: string;
  children: React.ReactNode;
  onEdit?: () => void | Promise<void>;
  editLabel?: string;
  disabled?: boolean;
}) {
  return (
    <div style={{ marginTop: 8, paddingTop: 8, borderTop: "1px solid #eee" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: "#333" }}>{title}</div>
        {onEdit ? (
          <button type="button" onClick={onEdit} disabled={disabled} style={{ padding: "6px 10px", fontSize: 12 }}>
            {editLabel}
          </button>
        ) : null}
      </div>
      <div style={{ marginTop: 6 }}>{children}</div>
    </div>
  );
}