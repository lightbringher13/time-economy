// src/pages/LoginPage.tsx
import { useState } from "react";
import { loginApi } from "../api/loginApi";
import { getMeApi } from "@/features/user/api/userApi";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuthStore } from "@/store/useAuthStore";
import { ROUTES } from "@/routes/paths";

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = useAuthStore((state) => state.login);
  const hydrateFromProfile = useAuthStore((s) => s.hydrateFromProfile);
  const from = (location.state as any)?.from?.pathname ?? ROUTES.DASHBOARD;
  const isAuthenticated = useAuthStore((s) => s.phase === "authenticated");
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const res = await loginApi({ email, password });

      // save access token for interceptor to use
      login(res.accessToken);

      try {
        const profile = await getMeApi();
        // 4) Map UserProfile → AuthUser and hydrate store
        hydrateFromProfile(profile);
      } catch (profileErr) {
        console.warn("[LoginPage] failed to load /me after login", profileErr);
        // optional: setUser(null); or just ignore
      }

      // later: update auth store + navigate
      console.log("Login success, accessToken:", res.accessToken);
      navigate(from, { replace: true });
      alert("Login success!");
    } catch (err) {
      console.error(err);
      setError("Login failed. Check your email/password.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: "40px auto" }}>
      <h1>Login</h1>
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

        {error && (
          <div style={{ color: "red", marginBottom: 8 }}>{error}</div>
        )}

        <button type="submit" disabled={loading}>
          {loading ? "Logging in..." : "Login"}
        </button>
        <p style={{ marginTop: 16 }}>
          Don't have an account?{" "}
          <button
            type="button"
            onClick={() => navigate(ROUTES.SIGNUP)}
            style={{
              border: "none",
              background: "none",
              color: "#0070f3",
              cursor: "pointer",
              padding: 0,
              textDecoration: "underline",
            }}
          >
            Register
          </button>
        </p>
        <p style={{ marginTop: 16 }}>
          Forgot password?{" "}
          <button
            type="button"
            onClick={() => navigate(ROUTES.FORGOT_PASSWORD)}
            style={{
              border: "none",
              background: "none",
              color: "#0070f3",
              cursor: "pointer",
              padding: 0,
              textDecoration: "underline",
            }}
          >
            Forgot_Password
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
      </form>
      <p>Auth status: {isAuthenticated ? "✅ Logged in" : "❌ Logged out"}</p>
    </div>
  );
};

export default LoginPage;