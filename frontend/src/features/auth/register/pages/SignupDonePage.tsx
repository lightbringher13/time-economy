// src/features/auth/register/pages/SignupDonePage.tsx
import { useLocation, useNavigate } from "react-router-dom";
import { SignupDoneStep } from "../components/SignupDoneStep";

export default function SignupDonePage() {
  const navigate = useNavigate();
  const location = useLocation();

  // optional message passed from navigate("/signup/done", { state: { message: "..." } })
  const message = (location.state as any)?.message ?? null;

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupDoneStep
        message={message}
        onGoToLogin={() => navigate("/login", { replace: true })}
      />
    </div>
  );
}