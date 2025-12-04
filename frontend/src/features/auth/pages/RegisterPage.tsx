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

  // ⭐ NEW FIELDS
  const [name, setName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [gender, setGender] = useState(""); // could be "MALE" | "FEMALE" | "OTHER"
  const [birthDate, setBirthDate] = useState(""); // yyyy-MM-dd (HTML date input)

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
    if (!name.trim()) {
      setError("Name is required.");
      return;
    }
    if (!phoneNumber.trim()) {
      setError("Phone number is required.");
      return;
    }
    if (!gender) {
      setError("Gender is required.");
      return;
    }
    if (!birthDate) {
      setError("Birth date is required.");
      return;
    }

    setLoading(true);
    try {
      await registerApi({
        email,
        password,
        phoneNumber,
        name,
        gender,
        birthDate, // "yyyy-MM-dd"
      });

      alert("Registration successful! Please log in.");
      navigate(ROUTES.LOGIN, { replace: true });
    } catch (err) {
      console.error("[RegisterPage] register failed", err);
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

        {/* ⭐ NEW: Name */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Name
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
        </div>

        {/* ⭐ NEW: Phone Number */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Phone Number
            <input
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            />
          </label>
        </div>

        {/* ⭐ NEW: Gender */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Gender
            <select
              value={gender}
              onChange={(e) => setGender(e.target.value)}
              style={{ display: "block", width: "100%", marginTop: 4 }}
            >
              <option value="">Select gender</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </label>
        </div>

        {/* ⭐ NEW: Birth Date */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Birth Date
            <input
              type="date"
              value={birthDate}
              onChange={(e) => setBirthDate(e.target.value)}
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