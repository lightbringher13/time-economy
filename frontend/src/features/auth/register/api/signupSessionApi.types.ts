// ✅ NEW: signup bootstrap response
export type SignupBootstrapResponse = {
  hasSession: boolean;
  email: string | null;
  emailVerified: boolean;
  phoneNumber: string | null;
  phoneVerified: boolean;
  name: string | null;
  gender: string | null;
  birthDate: string | null; // ISO yyyy-MM-dd from backend
  state: string | null;     // e.g. "EMAIL_PENDING", "EMAIL_VERIFIED"
};

export type UpdateSignupProfileRequest = {
  email: string;                // must not be null once autosave is triggered
  phoneNumber: string | null;
  name: string | null;
  gender: string | null;
  birthDate: string | null;     // yyyy-MM-dd
};