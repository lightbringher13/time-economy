// src/features/user/pages/DashboardPage.tsx

import { useAuthStore } from "@/store/useAuthStore";
import { Link } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <div style={{ maxWidth: 900, margin: "0 auto" }}>
      {/* Header */}
      <header style={{ marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Dashboard</h1>
        <p style={{ marginTop: 8, color: "#555" }}>
          {user
            ? `Welcome back, ${user.nickname || user.email}!`
            : "Welcome to TimeEconomy."}
        </p>
      </header>

      {/* Quick actions / navigation cards */}
      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
          gap: 16,
          marginBottom: 24,
        }}
      >
        <div
          style={{
            border: "1px solid #ddd",
            borderRadius: 8,
            padding: 16,
          }}
        >
          <h3 style={{ marginTop: 0 }}>Profile</h3>
          <p style={{ fontSize: 14, color: "#666" }}>
            View and update your account information.
          </p>
          <Link to={ROUTES.PROFILE}>Go to Profile →</Link>
        </div>

        <div
          style={{
            border: "1px solid #ddd",
            borderRadius: 8,
            padding: 16,
          }}
        >
          <h3 style={{ marginTop: 0 }}>Active Sessions</h3>
          <p style={{ fontSize: 14, color: "#666" }}>
            Check which devices are logged in and manage session security.
          </p>
          <Link to={ROUTES.SESSIONS}>View Sessions →</Link>
        </div>

        <div
          style={{
            border: "1px solid #ddd",
            borderRadius: 8,
            padding: 16,
          }}
        >
          <h3 style={{ marginTop: 0 }}>System Health</h3>
          <p style={{ fontSize: 14, color: "#666" }}>
            Make sure the backend is healthy and responding.
          </p>
          <Link to={ROUTES.HEALTH}>Health Check →</Link>
        </div>
      </section>

      {/* Placeholder for future “real” dashboard stats */}
      <section
        style={{
          border: "1px solid #eee",
          borderRadius: 8,
          padding: 16,
        }}
      >
        <h2 style={{ marginTop: 0 }}>Overview</h2>
        <p style={{ fontSize: 14, color: "#666" }}>
          This is where you can later show:
        </p>
        <ul style={{ fontSize: 14, color: "#666" }}>
          <li>Recent transactions / activities</li>
          <li>Expense summary (this month vs last month)</li>
          <li>Security alerts or login history</li>
        </ul>
      </section>
    </div>
  );
}