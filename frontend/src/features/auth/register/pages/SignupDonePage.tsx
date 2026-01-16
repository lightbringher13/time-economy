// src/features/auth/register/pages/SignupDonePage.tsx
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";
import { SignupDoneStep } from "../components/SignupDoneStep";

export default function SignupDonePage() {
  const navigate = useNavigate();

  // optional: auto redirect after a short delay
  // useEffect(() => {
  //   const t = setTimeout(() => navigate(ROUTES.LOGIN, { replace: true }), 1500);
  //   return () => clearTimeout(t);
  // }, [navigate]);

  const onGoToLogin = () => {
    navigate(ROUTES.LOGIN, { replace: true });
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupDoneStep onGoToLogin={onGoToLogin} />
    </div>
  );
}