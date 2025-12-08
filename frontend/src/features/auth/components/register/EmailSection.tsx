// features/auth/components/register/EmailSection.tsx
import { useFormContext } from "react-hook-form";
import type { RegisterFormValues } from "../../types/auth";

type EmailSectionProps = {
  emailVerified: boolean;
  sendEmailCodeLoading: boolean;
  verifyEmailCodeLoading: boolean;
  verificationInfo: string | null;
  onSendCode: () => void | Promise<void>;
  onVerifyCode: () => void | Promise<void>;
};

export function EmailSection({
  emailVerified,
  sendEmailCodeLoading,
  verifyEmailCodeLoading,
  verificationInfo,
  onSendCode,
  onVerifyCode,
}: EmailSectionProps) {
  const {
    register,
    formState: { errors },
  } = useFormContext<RegisterFormValues>();

  return (
    <>
      {/* Email + Send Code */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Email
          <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
            <input
              type="email"
              {...register("email")}
              disabled={emailVerified}
              style={{ flex: 1 }}
            />
            <button
              type="button"
              onClick={onSendCode}
              disabled={sendEmailCodeLoading || emailVerified}
            >
              {sendEmailCodeLoading ? "Sending..." : "Send Code"}
            </button>
          </div>
        </label>

        {/* ⭐ Zod/RHF error: email validation */}
        {errors.email && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.email.message as string}
          </div>
        )}

        {/* ⭐ Verified (business logic) */}
        {emailVerified && (
          <div style={{ color: "green", marginTop: 4 }}>✅ Email verified</div>
        )}
      </div>

      {/* Email Verification Code */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Email Verification Code
          <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
            <input
              type="text"
              {...register("emailCode")}
              disabled={emailVerified}
              style={{ flex: 1 }}
            />
            <button
              type="button"
              onClick={onVerifyCode}
              disabled={verifyEmailCodeLoading || emailVerified}
            >
              {verifyEmailCodeLoading ? "Verifying..." : "Verify"}
            </button>
          </div>
        </label>

        {/* ⭐ Zod/RHF error: emailCode validation */}
        {errors.emailCode && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.emailCode.message as string}
          </div>
        )}
      </div>

      {/* ⭐ Section-level business messages */}
      {verificationInfo && (
        <div style={{ color: "#555", fontSize: 12, marginBottom: 8 }}>
          {verificationInfo}
        </div>
      )}
    </>
  );
}