// src/shared/components/AuthDebugPanel.tsx
import { useAuthStore } from "@/store/useAuthStore";

export function AuthDebugPanel() {
  const phase = useAuthStore((s) => s.phase);
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);

  return (
    <div style={{ marginTop: 20, padding: 12, border: "1px solid #ccc" }}>
      <h3>Auth Debug Panel</h3>

      <p>
        <strong>Phase:</strong> {phase}
      </p>

      <p>
        <strong>User:</strong> {user ? JSON.stringify(user) : "null"}
      </p>

      <p>
        <strong>Access Token (short):</strong>{" "}
        {accessToken ? accessToken.slice(0, 30) + "..." : "null"}
      </p>
    </div>
  );
}