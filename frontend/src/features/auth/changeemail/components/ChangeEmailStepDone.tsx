// src/features/auth/change-email/components/ChangeEmailStepDone.tsx
interface Props {
  newEmail?: string | null; // show if present, otherwise just show success message
  onClose?: () => void;
}

export function ChangeEmailStepDone({ newEmail, onClose }: Props) {
  return (
    <div>
      <h2>Email change complete</h2>

      <p style={{ marginTop: 8, color: "#333" }}>
        Your email has been successfully updated.
      </p>

      {newEmail && (
        <p style={{ marginTop: 8, fontSize: 14 }}>
          Updated email: <strong>{newEmail}</strong>
        </p>
      )}

      {onClose && (
        <button onClick={onClose} style={{ marginTop: 16, padding: "8px 14px" }}>
          Close
        </button>
      )}
    </div>
  );
}