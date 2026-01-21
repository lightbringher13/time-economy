import type { SignupSessionState } from "../api/signupApi.types";
import { ROUTES } from "@/routes/paths";

export function signupPathFromState(state: SignupSessionState | null) {
  switch (state) {
    // 1) Start / draft -> email entry
    case null:
    case "DRAFT":
      return ROUTES.SIGNUP_EMAIL;

    // 2) Email OTP pending -> email verify page
    case "EMAIL_OTP_SENT":
      return ROUTES.SIGNUP_VERIFY_EMAIL; // "/signup/verify/email"

    // 3) Email verified -> phone entry
    case "EMAIL_VERIFIED":
      return ROUTES.SIGNUP_PHONE;

    // 4) Phone OTP pending -> phone verify page
    case "PHONE_OTP_SENT":
      return ROUTES.SIGNUP_VERIFY_PHONE; // "/signup/verify/phone"

    // (optional safety) if this ever appears, treat it as “move forward”
    case "PHONE_VERIFIED":
      return ROUTES.SIGNUP_PROFILE;
      
    // 6) Review
    case "PROFILE_READY":
      return ROUTES.SIGNUP_REVIEW;

    // 7) Done (outside /signup layout tree)
    case "COMPLETED":
      return ROUTES.SIGNUP_DONE;

    case "CANCELED":
      return ROUTES.SIGNUP_CANCELED;

    case "EXPIRED":
    default:
      return ROUTES.SIGNUP_EXPIRED;
  }
}