import { useEffect, useState } from "react";
import {
  getSessionsApi,
  revokeSessionApi,
} from "@/features/user/api/userApi";
import type { SessionInfo } from "@/features/user/types/user";

function formatDate(value: string | null) {
  if (!value) return "-";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString();
}

export default function SessionsPage() {
  const [sessions, setSessions] = useState<SessionInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [revokingId, setRevokingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 🔁 Load sessions on mount
  useEffect(() => {
    let cancelled = false;

    async function loadSessions() {
      setLoading(true);
      setError(null);
      try {
        const data = await getSessionsApi();
        if (cancelled) return;
        setSessions(data);
      } catch (e) {
        console.error("[SessionsPage] failed to load sessions", e);
        if (!cancelled) {
          setError("Failed to load sessions. Please try again.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadSessions();

    return () => {
      cancelled = true;
    };
  }, []);

  // 🧯 Revoke a specific session
  const handleRevoke = async (id: number) => {
    setError(null);
    setRevokingId(id);
    try {
      await revokeSessionApi(id);
      // remove from list (or mark revoked)
      setSessions((prev) =>
        prev.map((s) =>
          s.id === id ? { ...s, revoked: true } : s
        )
      );
    } catch (e) {
      console.error("[SessionsPage] failed to revoke session", e);
      setError("Failed to revoke session. Please try again.");
    } finally {
      setRevokingId(null);
    }
  };

  const handleRefreshList = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getSessionsApi();
      setSessions(data);
    } catch (e) {
      console.error("[SessionsPage] refresh failed", e);
      setError("Failed to refresh sessions.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: "40px auto" }}>
      <h1>Active Sessions</h1>
      <p>
        These are your logged-in devices / browsers. You can revoke sessions
        you don’t recognize.
      </p>

      <div style={{ margin: "12px 0" }}>
        <button onClick={handleRefreshList} disabled={loading}>
          {loading ? "Loading..." : "Reload sessions"}
        </button>
      </div>

      {error && (
        <div style={{ color: "red", marginBottom: 12 }}>{error}</div>
      )}

      {sessions.length === 0 && !loading && (
        <p>No active sessions found.</p>
      )}

      {sessions.length > 0 && (
        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            marginTop: 16,
          }}
        >
          <thead>
            <tr>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                Device
              </th>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                IP
              </th>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                User Agent
              </th>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                Created
              </th>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                Last Used
              </th>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                Expires
              </th>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                Status
              </th>
              <th style={{ borderBottom: "1px solid #ddd", padding: 8 }}>
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {sessions.map((s) => {
              const isCurrent = s.current;
              const isRevoked = s.revoked;

              return (
                <tr key={s.id}>
                  <td style={{ borderBottom: "1px solid #eee", padding: 8 }}>
                    {s.deviceInfo || "-"}
                  </td>
                  <td style={{ borderBottom: "1px solid #eee", padding: 8 }}>
                    {s.ipAddress || "-"}
                  </td>
                  <td
                    style={{
                      borderBottom: "1px solid #eee",
                      padding: 8,
                      maxWidth: 200,
                      wordBreak: "break-all",
                      fontSize: 12,
                    }}
                  >
                    {s.userAgent}
                  </td>
                  <td style={{ borderBottom: "1px solid #eee", padding: 8 }}>
                    {formatDate(s.createdAt)}
                  </td>
                  <td style={{ borderBottom: "1px solid #eee", padding: 8 }}>
                    {formatDate(s.lastUsedAt)}
                  </td>
                  <td style={{ borderBottom: "1px solid #eee", padding: 8 }}>
                    {formatDate(s.expiresAt)}
                  </td>
                  <td style={{ borderBottom: "1px solid #eee", padding: 8 }}>
                    {isRevoked
                      ? "Revoked"
                      : isCurrent
                      ? "Current device"
                      : "Active"}
                  </td>
                  <td style={{ borderBottom: "1px solid #eee", padding: 8 }}>
                    
                      <button
                        onClick={() => handleRevoke(s.id)}
                        disabled={isRevoked || revokingId === s.id}
                      >
                        {revokingId === s.id
                          ? "Revoking..."
                          : isRevoked
                          ? "Revoked"
                          : "Logout"}
                      </button>
                    
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}