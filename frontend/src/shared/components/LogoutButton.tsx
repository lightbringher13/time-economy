// src/shared/components/LogoutButton.tsx
import { useNavigate } from "react-router-dom";
import { logoutApi } from "@/features/auth/api/authApi";
import { useAuthStore } from "@/store/useAuthStore";
import { ROUTES } from "@/routes/paths";

export function LogoutButton() {
  const logoutStore = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logoutApi();     // tell backend to revoke cookie/token
    } catch (e) {
      console.warn("Logout API failed, still clearing local state", e);
    } finally {
      logoutStore();         // clear Zustand state
      navigate(ROUTES.LOGIN);
    }
  };

  return (
    <button
      onClick={handleLogout}
      style={{ marginLeft: 12 }}
    >
      Logout
    </button>
  );
}