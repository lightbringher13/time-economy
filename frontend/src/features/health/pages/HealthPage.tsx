// src/features/health/pages/HealthPage.tsx
import { useEffect, useState } from "react";
import { getHealthApi } from "../api/healthApi";
import type { HealthResponse } from "../types/HealthResponse";

export default function HealthPage() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getHealthApi();
      setHealth(data);
    } catch (e) {
      console.error("[HealthPage] health check failed", e);
      setError("Health check failed. See console for details.");
      setHealth(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHealth();
  }, []);

  return (
    <div style={{ maxWidth: 600, margin: "40px auto" }}>
      <h1>Backend Health</h1>
      <p>
        This page calls <code>GET /api/health</code> via the gateway to confirm:
        <br />
        - gateway is running,
        <br />
        - routing works,
        <br />
        - CORS / baseURL / cookies are configured correctly.
      </p>

      <button onClick={loadHealth} disabled={loading} style={{ margin: "12px 0" }}>
        {loading ? "Checking..." : "Re-check health"}
      </button>

      {error && (
        <div style={{ color: "red", marginTop: 8 }}>
          {error}
        </div>
      )}

      {health && !error && (
        <div
          style={{
            marginTop: 16,
            padding: 12,
            border: "1px solid #ccc",
            borderRadius: 4,
            background: "#f9f9f9",
          }}
        >
          <strong>Gateway:</strong> {health.service}
          <br />
          <strong>Status:</strong> {health.status}
          <br />
          <strong>Success:</strong> {health.success ? "true" : "false"}
          <br />
          <strong>Timestamp:</strong> {new Date(health.timestamp).toLocaleString()}
        </div>
      )}
    </div>
  );
}