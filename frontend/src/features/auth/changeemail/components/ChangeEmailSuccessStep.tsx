// src/features/auth/change-email/components/ChangeEmailSuccessStep.tsx
import React from "react";

interface ChangeEmailSuccessStepProps {
  newEmail: string | null;
  onLogoutRedirect?: () => void;   // e.g. go to /login
  onClose?: () => void;            // e.g. close modal / go back to settings
}

export const ChangeEmailSuccessStep: React.FC<ChangeEmailSuccessStepProps> = ({
  newEmail,
  onLogoutRedirect,
  onClose,
}) => {
  return (
    <div>
      <h2>이메일 변경 완료</h2>

      <p style={{ marginBottom: "0.75rem", fontSize: "0.95rem" }}>
        계정 이메일이 다음 주소로 변경되었습니다:
      </p>

      <p
        style={{
          fontWeight: "bold",
          marginBottom: "1rem",
          fontSize: "1rem",
          wordBreak: "break-all",
        }}
      >
        {newEmail ?? "새 이메일"}
      </p>

      <p style={{ fontSize: "0.85rem", color: "#555", marginBottom: "1rem" }}>
        보안을 위해 현재 로그인 세션을 포함한 모든 기기에서 로그아웃됩니다.
        <br />
        새 이메일로 다시 로그인해 주세요.
      </p>

      <div style={{ display: "flex", gap: "0.5rem" }}>
        {onLogoutRedirect && (
          <button
            type="button"
            onClick={onLogoutRedirect}
            style={{ padding: "0.5rem 1rem" }}
          >
            로그인 화면으로 이동
          </button>
        )}

        {onClose && (
          <button
            type="button"
            onClick={onClose}
            style={{ padding: "0.5rem 1rem" }}
          >
            닫기
          </button>
        )}
      </div>
    </div>
  );
};