// features/auth/components/register/PhoneSection.tsx
import { useFormContext } from "react-hook-form";
import type { RegisterFormValues } from "../../types/auth";

type PhoneSectionProps = {
  phoneVerified: boolean;
  sendPhoneCodeLoading: boolean;
  verifyPhoneCodeLoading: boolean;
  phoneVerificationInfo: string | null;
  onSendPhoneCode: () => void | Promise<void>;
  onVerifyPhoneCode: () => void | Promise<void>;
};

export function PhoneSection({
  phoneVerified,
  sendPhoneCodeLoading,
  verifyPhoneCodeLoading,
  phoneVerificationInfo,
  onSendPhoneCode,
  onVerifyPhoneCode,
}: PhoneSectionProps) {
  const {
    register,
    formState: { errors },
  } = useFormContext<RegisterFormValues>();

  return (
    <>
      {/* Phone Number + Send Phone Code */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Phone Number
          <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
            <input
              type="tel"
              {...register("phoneNumber")}
              disabled={phoneVerified}
              style={{ flex: 1 }}
            />
            <button
              type="button"
              onClick={onSendPhoneCode}
              disabled={sendPhoneCodeLoading || phoneVerified}
            >
              {sendPhoneCodeLoading ? "Sending..." : "Send Code"}
            </button>
          </div>
        </label>

        {/* ⭐ Zod/RHF error for phoneNumber */}
        {errors.phoneNumber && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.phoneNumber.message as string}
          </div>
        )}

        {phoneVerified && (
          <div style={{ color: "green", marginTop: 4 }}>✅ Phone verified</div>
        )}
      </div>

      {/* Phone Verify Code */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Phone Verification Code
          <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
            <input
              type="text"
              {...register("phoneCode")}
              disabled={phoneVerified}
              style={{ flex: 1 }}
            />
            <button
              type="button"
              onClick={onVerifyPhoneCode}
              disabled={verifyPhoneCodeLoading || phoneVerified}
            >
              {verifyPhoneCodeLoading ? "Verifying..." : "Verify"}
            </button>
          </div>
        </label>

        {/* ⭐ Zod/RHF error for phoneCode */}
        {errors.phoneCode && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.phoneCode.message as string}
          </div>
        )}
      </div>

      {phoneVerificationInfo && (
        <div style={{ color: "#555", fontSize: 12, marginBottom: 8 }}>
          {phoneVerificationInfo}
        </div>
      )}
    </>
  );
}