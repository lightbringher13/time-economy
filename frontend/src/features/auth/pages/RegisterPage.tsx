import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  registerApi,
  sendEmailCodeApi,
  verifyEmailCodeApi,
  signupBootstrapApi,
  updateSignupProfileApi,
} from "../api/authApi";
import { useAuthStore } from "@/store/useAuthStore";
import { ROUTES } from "@/routes/paths";

export default function RegisterPage() {
  const navigate = useNavigate();
  const phase = useAuthStore((s) => s.phase);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");

  // Profile fields
  const [name, setName] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [gender, setGender] = useState("");
  const [birthDate, setBirthDate] = useState(""); // yyyy-MM-dd

  // Email verification
  const [emailCode, setEmailCode] = useState("");
  const [emailVerified, setEmailVerified] = useState(false);
  const [sendCodeLoading, setSendCodeLoading] = useState(false);
  const [verifyCodeLoading, setVerifyCodeLoading] = useState(false);
  const [verificationInfo, setVerificationInfo] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [bootstrapLoading, setBootstrapLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [hasSignupSession, setHasSignupSession] = useState(false);

  // debounce timer for autosave
  const autosaveTimerRef = useRef<number | null>(null);

  // 🔒 If already logged in, no need to register → go to dashboard
  useEffect(() => {
    if (phase === "authenticated") {
      navigate(ROUTES.DASHBOARD, { replace: true });
    }
  }, [phase, navigate]);

  // ⭐ 1) BOOTSTRAP signup session on page load
  useEffect(() => {
    const bootstrap = async () => {
      try {
        setBootstrapLoading(true);
        const data = await signupBootstrapApi();

        setHasSignupSession(data.hasSession);

        if (data.hasSession) {
          if (data.email) setEmail(data.email);
          if (data.name) setName(data.name);
          if (data.phoneNumber) setPhoneNumber(data.phoneNumber);
          if (data.gender) setGender(data.gender);
          if (data.birthDate) setBirthDate(data.birthDate);
          setEmailVerified(data.emailVerified);
        }
      } catch (err) {
        console.error("[RegisterPage] signup bootstrap failed", err);
        // we can silently ignore and treat as fresh signup
      } finally {
        setBootstrapLoading(false);
      }
    };

    bootstrap();
  }, []);

  // ⭐ 2) AUTOSAVE profile to signup session (debounced)
  useEffect(() => {
    // 조건:
    // - bootstrap 완료 후
    // - signup session 존재할 때만
    if (bootstrapLoading || !hasSignupSession) return;

    // 이메일이 아직 없으면 굳이 저장할 필요 없음
    if (!email.trim()) return;

    // debounce 600ms
    if (autosaveTimerRef.current) {
      window.clearTimeout(autosaveTimerRef.current);
    }

    autosaveTimerRef.current = window.setTimeout(() => {
      updateSignupProfileApi({
        name: name || null,
        phoneNumber: phoneNumber || null,
        gender: gender || null,
        birthDate: birthDate || null,
      }).catch((err) => {
        console.error("[RegisterPage] autosave profile failed", err);
        // 실서비스라면 여기서 토스트 정도 띄워줄 수도 있음
      });
    }, 600);

    return () => {
      if (autosaveTimerRef.current) {
        window.clearTimeout(autosaveTimerRef.current);
      }
    };
  }, [bootstrapLoading, hasSignupSession, email, name, phoneNumber, gender, birthDate]);

  const handleSendCode = async () => {
    setError(null);
    setVerificationInfo(null);

    if (!email.trim()) {
      setError("Email is required before sending code.");
      return;
    }

    setSendCodeLoading(true);
    try {
      const code = await sendEmailCodeApi({ email });
      setVerificationInfo(`Verification code (dev): ${code}`);
    } catch (err) {
      console.error("[RegisterPage] send email code failed", err);
      setError("Failed to send verification code. Please try again.");
    } finally {
      setSendCodeLoading(false);
    }
  };

  const handleVerifyCode = async () => {
    setError(null);
    setVerificationInfo(null);

    if (!email.trim()) {
      setError("Email is required.");
      return;
    }
    if (!emailCode.trim()) {
      setError("Verification code is required.");
      return;
    }

    setVerifyCodeLoading(true);
    try {
      const res = await verifyEmailCodeApi({ email, code: emailCode });
      if (res.verified) {
        setEmailVerified(true);
        setVerificationInfo("Email verified successfully.");
      } else {
        setEmailVerified(false);
        setError("Invalid or expired verification code.");
      }
    } catch (err) {
      console.error("[RegisterPage] verify email code failed", err);
      setError("Failed to verify code. Please try again.");
      setEmailVerified(false);
    } finally {
      setVerifyCodeLoading(false);
    }
  };

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
    if (!emailVerified) {
      setError("Please verify your email before registering.");
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
        birthDate,
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

      {bootstrapLoading && <p>Loading signup info...</p>}

      <form onSubmit={handleSubmit}>
        {/* Email + Send Code */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Email
            <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
              <input
                type="email"
                value={email}
                disabled={emailVerified} // certified email locked
                onChange={(e) => setEmail(e.target.value)}
                style={{ flex: 1 }}
              />
              <button
                type="button"
                onClick={handleSendCode}
                disabled={sendCodeLoading || emailVerified}
              >
                {sendCodeLoading ? "Sending..." : "Send Code"}
              </button>
            </div>
          </label>
          {emailVerified && (
            <div style={{ color: "green", marginTop: 4 }}>
              ✅ Email verified
            </div>
          )}
        </div>

        {/* Verify Code */}
        <div style={{ marginBottom: 12 }}>
          <label>
            Verification Code
            <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
              <input
                type="text"
                value={emailCode}
                onChange={(e) => setEmailCode(e.target.value)}
                disabled={emailVerified}
                style={{ flex: 1 }}
              />
              <button
                type="button"
                onClick={handleVerifyCode}
                disabled={verifyCodeLoading || emailVerified}
              >
                {verifyCodeLoading ? "Verifying..." : "Verify"}
              </button>
            </div>
          </label>
        </div>

        {verificationInfo && (
          <div style={{ color: "#555", fontSize: 12, marginBottom: 8 }}>
            {verificationInfo}
          </div>
        )}

        {/* Passwords */}
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

        {/* Name */}
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

        {/* Phone Number */}
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

        {/* Gender */}
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

        {/* Birth Date */}
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