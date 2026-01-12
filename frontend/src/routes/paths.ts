// src/routes/paths.ts
export const ROUTES = {
  DASHBOARD: "/dashboard",
  LOGIN: "/login",

  // ✅ Signup (multi-step)
  SIGNUP: "/signup",
  SIGNUP_EMAIL: "/signup/email",
  SIGNUP_EMAIL_EDIT: "/signup/edit/email",
  SIGNUP_PHONE: "/signup/phone",
  SIGNUP_PHONE_EDIT: "/signup/edit/phone",
  SIGNUP_PROFILE: "/signup/profile",
  SIGNUP_REVIEW: "/signup/review",
  SIGNUP_DONE: "/signup/done",
  SIGNUP_CANCELED: "/signup/canceled",
  SIGNUP_EXPIRED: "/signup/expired",

  PROFILE: "/profile",
  SESSIONS: "/sessions",
  HEALTH: "/health",
  FORGOT_PASSWORD: "/forgot-password",
  RESET_PASSWORD: "/reset-password",
  CHANGE_PASSWORD: "/change-password",
  CHANGE_EMAIL: "/change-email",
} as const;