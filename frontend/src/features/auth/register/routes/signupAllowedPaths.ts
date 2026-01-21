// signupAllowedPaths.ts (or inside SignupLayout file)
import { ROUTES } from "@/routes/paths";
import type { SignupSessionState } from "../api/signupApi.types";

export const allowedPathsByState: Record<SignupSessionState, Set<string>> = {
  DRAFT: new Set([
    ROUTES.SIGNUP_EMAIL,
  ]),

  EMAIL_OTP_SENT: new Set([
    ROUTES.SIGNUP_EMAIL,         // allow back to edit email input / resend
    ROUTES.SIGNUP_VERIFY_EMAIL,  // verify page
  ]),

  EMAIL_VERIFIED: new Set([
    ROUTES.SIGNUP_EMAIL,         // allow back (should show “already verified”)
    ROUTES.SIGNUP_VERIFY_EMAIL,  // allow back (should show “already verified”)
    ROUTES.SIGNUP_PHONE,         // current step
  ]),

  PHONE_OTP_SENT: new Set([
    ROUTES.SIGNUP_EMAIL,
    ROUTES.SIGNUP_VERIFY_EMAIL,
    ROUTES.SIGNUP_PHONE,
    ROUTES.SIGNUP_VERIFY_PHONE,
  ]),

  PHONE_VERIFIED: new Set([
    ROUTES.SIGNUP_EMAIL,
    ROUTES.SIGNUP_VERIFY_EMAIL,
    ROUTES.SIGNUP_PHONE,
    ROUTES.SIGNUP_VERIFY_PHONE,
    ROUTES.SIGNUP_PROFILE,
  ]),

  PROFILE_READY: new Set([
    ROUTES.SIGNUP_EMAIL,
    ROUTES.SIGNUP_VERIFY_EMAIL,
    ROUTES.SIGNUP_PHONE,
    ROUTES.SIGNUP_VERIFY_PHONE,
    ROUTES.SIGNUP_PROFILE,
    ROUTES.SIGNUP_REVIEW,
  ]),

  COMPLETED: new Set([
    ROUTES.SIGNUP_DONE,
  ]),

  CANCELED: new Set([
    ROUTES.SIGNUP_CANCELED,
  ]),

  EXPIRED: new Set([
    ROUTES.SIGNUP_EXPIRED,
  ]),
};