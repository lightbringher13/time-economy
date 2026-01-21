// src/features/auth/register/components/SignupDoneStep.tsx
interface Props {
  loading?: boolean;
  message?: string | null;

  // If you force redirect to login (most common), you can just show a button as fallback.
  onGoToLogin?: () => void | Promise<void>;
}

export function SignupDoneStep({ loading, message, onGoToLogin }: Props) {
  return (
    <div>
      <h2>Signup complete</h2>

      <p style={{ marginTop: 8, color: "#333" }}>
        Your account has been created successfully.
      </p>

      {message && (
        <p style={{ marginTop: 8, fontSize: 14, color: "#666" }}>
          {message}
        </p>
      )}

      {onGoToLogin && (
        <button
          type="button"
          onClick={onGoToLogin}
          disabled={!!loading}
          style={{ marginTop: 16, padding: "8px 14px" }}
        >
          {loading ? "Redirecting..." : "Go to login"}
        </button>
      )}
    </div>
  );
}