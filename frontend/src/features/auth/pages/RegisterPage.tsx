import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { registerApi } from "../api/authApi";
import { useAuthStore } from "@/store/useAuthStore";
import { ROUTES } from "@/routes/paths";

export default function RegisterPage() {
  const navigate = useNavigate();
  const phase = useAuthStore((s) => s.phase);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 🔒 If already logged in, no need to register → go to dashboard
  useEffect(() => {
    if (phase === "authenticated") {
      navigate(ROUTES.DASHBOARD, { replace: true });
    }
  }, [phase, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.trim()) {
      setError("Email is required.");
      return;
    }
    if (!password) {
      setError("Password is required.");
      return;
    }
    if (password !== passwordConfirm) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      await registerApi({ email, password });

      // You can remove alert later and replace with a toast
      alert("Registration successful! Please log in.");
      navigate(ROUTES.LOGIN, { replace: true });
    } catch (err) {
      console.error("[RegisterPage] register failed", err);
      // GlobalExceptionHandler + apiClient interceptor will shape error,
      // but for now we just show a generic message.
      setError("Failed to register. Please check your information.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: "40px auto" }}>
      <h1>Register</h1>
      <p style={{ marginBottom: 16 }}>
        Create a TimeEconomy account to start tracking your expenses.
      </p>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
        </div>

        <div style={{ marginBottom: 12 }}>
          <label>
            Confirm Password
            <input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
        </div>

        {error && (
          <div style={{ color: "red", marginBottom: 8 }}>{error}</div>
        )}

        <button type="submit" disabled={loading} style={{ width: "100%" }}>
          {loading ? "Registering..." : "Register"}
        </button>
      </form>

      <p style={{ marginTop: 16 }}>
        Already have an account?{" "}
        <button
          type="button"
          onClick={() => navigate(ROUTES.LOGIN)}
          style={{
            border: "none",
            background: "none",
            color: "#0070f3",
            cursor: "pointer",
            padding: 0,
            textDecoration: "underline",
          }}
        >
          Log in
        </button>
      </p>
      <p style={{ marginTop: 16 }}>
        Backend Health Check?{" "}
        <button
          type="button"
          onClick={() => navigate(ROUTES.HEALTH)}
          style={{
            border: "none",
            background: "none",
            color: "#0070f3",
            cursor: "pointer",
            padding: 0,
            textDecoration: "underline",
          }}
        >
          HealthCheck
        </button>
      </p>
    </div>
  );
}