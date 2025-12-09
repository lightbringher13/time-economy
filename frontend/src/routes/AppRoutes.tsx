// src/routes/AppRoutes.tsx

import { Routes, Route, Navigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";
import { ProtectedRoute } from "./ProtectedRoute";

import LoginPage from "@/features/auth/pages/LoginPage";
import RegisterPage from "@/features/auth/pages/RegisterPage";
import DashboardPage from "@/features/user/pages/DashboardPage";
import ProfilePage from "@/features/user/pages/ProfilePage";
import SessionsPage from "@/features/user/pages/SessionsPage";
import HealthPage from "@/features/health/pages/HealthPage";
import ForgotPasswordPage from "@/features/auth/pages/ForgotPasswordPage";
import ResetPasswordPage from "@/features/auth/pages/ResetPasswordPage";
import ChangePasswordPage from "@/features/auth/pages/ChangePasswordPage";

export function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.REGISTER} element={<RegisterPage />} />
      <Route path={ROUTES.HEALTH} element={<HealthPage />} />
      <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPasswordPage />} />
      <Route path={ROUTES.RESET_PASSWORD} element={<ResetPasswordPage />} />

      {/* Protected routes */}
      <Route element={<ProtectedRoute />}>
        <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
        <Route path={ROUTES.PROFILE} element={<ProfilePage />} />
        <Route path={ROUTES.SESSIONS} element={<SessionsPage />} />
        <Route path={ROUTES.CHANGE_PASSWORD} element={<ChangePasswordPage />} />
      </Route>

      {/* Fallback: root → dashboard */}
      <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />

      {/* Any unknown path → dashboard (or login, your choice) */}
      <Route path="*" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
    </Routes>
  );
}