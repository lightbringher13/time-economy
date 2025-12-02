import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthStore } from "@/store/useAuthStore";
import { ROUTES } from "./paths";
import { LogoutButton } from "@/shared/components/LogoutButton";

export function ProtectedRoute() {
  const phase = useAuthStore((s) => s.phase);
  const location = useLocation();

  console.log("[ProtectedRoute] phase =", phase, "path =", location.pathname);

  if (phase === "bootstrapping") {
    return <div>Loading...</div>;
  }

  if (phase !== "authenticated") {
    return (
      <Navigate
        to={ROUTES.LOGIN}
        replace
        state={{ from: location }}
      />
    );
  }

  return (
    <div>
      {/* Top bar */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          padding: "12px 20px",
          borderBottom: "1px solid #eee",
        }}
      >
        <strong>TimeEconomy</strong>
        <LogoutButton />
      </div>

      {/* Page content */}
      <div>
        <Outlet />
      </div>
    </div>
  );
}


