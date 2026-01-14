// src/routes/AppRoutes.tsx
import { Routes, Route, Navigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";
import { ProtectedRoute } from "./ProtectedRoute";

import LoginPage from "@/features/auth/login/pages/LoginPage";
import DashboardPage from "@/features/user/pages/DashboardPage";
import ProfilePage from "@/features/user/pages/ProfilePage";
import SessionsPage from "@/features/user/pages/SessionsPage";
import HealthPage from "@/features/health/pages/HealthPage";
import ForgotPasswordPage from "@/features/auth/passwordreset/pages/ForgotPasswordPage";
import ResetPasswordPage from "@/features/auth/passwordreset/pages/ResetPasswordPage";
import ChangePasswordPage from "@/features/auth/changepassword/pages/ChangePasswordPage";
import ChangeEmailPage from "@/features/auth/changeemail/pages/ChangeEmailPage";

// ✅ Signup pages (PUBLIC)
import SignupLayout from "@/features/auth/register/pages/SignupLayout";
import SignupEmailPage  from "@/features/auth/register/pages/SignupEmailPage";
import SignupEmailEditPage from "@/features/auth/register/pages/SignupEmailEditPage";
import SignupPhonePage from "@/features/auth/register/pages/SignupPhonePage";
import SignupPhoneEditPage from "@/features/auth/register/pages/SignupPhoneEditPage";
import SignupProfilePage from "@/features/auth/register/pages/SignupProfilePage";
import SignupReviewPage from "@/features/auth/register/pages/SignupReviewPage";
import SignupDonePage from "@/features/auth/register/pages/SignupDonePage";
// import { SignupCanceledPage } from "@/features/auth/register/pages/SignupCanceledPage";
// import { SignupExpiredPage } from "@/features/auth/register/pages/SignupExpiredPage";

export function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.HEALTH} element={<HealthPage />} />
      <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPasswordPage />} />
      <Route path={ROUTES.RESET_PASSWORD} element={<ResetPasswordPage />} />

      {/* ✅ Signup routes (public) */}
      <Route path={ROUTES.SIGNUP} element={<SignupLayout />}>
        {/* /signup -> /signup/email */}
        <Route index element={<Navigate to={ROUTES.SIGNUP_EMAIL} replace />} />

        {/* child paths must be RELATIVE */}
        <Route path="email" element={<SignupEmailPage />} />
        <Route path="edit/email" element={<SignupEmailEditPage />} />
        <Route path="phone" element={<SignupPhonePage />} />
        <Route path="edit/phone" element={<SignupPhoneEditPage />} />
        <Route path="profile" element={<SignupProfilePage />} />
        <Route path="review" element={<SignupReviewPage />} />
        

        {/* optional */}
        {/* <Route path="canceled" element={<SignupCanceledPage />} /> */}
        {/* <Route path="expired" element={<SignupExpiredPage />} /> */}
      </Route>

      <Route path="done" element={<SignupDonePage />} />

      {/* Protected routes */}
      <Route element={<ProtectedRoute />}>
        <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
        <Route path={ROUTES.PROFILE} element={<ProfilePage />} />
        <Route path={ROUTES.SESSIONS} element={<SessionsPage />} />
        <Route path={ROUTES.CHANGE_PASSWORD} element={<ChangePasswordPage />} />
        <Route path={ROUTES.CHANGE_EMAIL} element={<ChangeEmailPage />} />
      </Route>

      {/* Fallback */}
      <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
      <Route path="*" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
    </Routes>
  );
}