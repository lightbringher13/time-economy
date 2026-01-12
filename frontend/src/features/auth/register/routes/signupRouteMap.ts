import type { SignupSessionState } from "../api/signupApi.types";

export function signupPathFromState(state: SignupSessionState | null) {
  switch (state) {
    case "DRAFT":
    case "EMAIL_OTP_SENT":
      return "/signup/email";

    case "EMAIL_VERIFIED":
    case "PHONE_OTP_SENT":
      return "/signup/phone";

    case "PROFILE_PENDING":
      return "/signup/profile";

    case "PROFILE_READY":
      return "/signup/review";

    case "COMPLETED":
      return "/signup/done";

    case "CANCELED":
      return "/signup/canceled";

    case "EXPIRED":
    default:
      return "/signup/expired";
  }
}