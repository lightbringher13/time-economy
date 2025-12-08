// features/auth/components/register/PasswordSection.tsx
import { useFormContext } from "react-hook-form";
import type { RegisterFormValues } from "../../types/auth";

export function PasswordSection() {
  const {
    register,
    formState: { errors },
  } = useFormContext<RegisterFormValues>();

  return (
    <>
      {/* Password */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Password
          <input
            type="password"
            {...register("password")}
            style={{ display: "block", width: "100%", marginTop: 4 }}
          />
        </label>

        {/* ⭐ Zod/RHF error for password */}
        {errors.password && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.password.message as string}
          </div>
        )}
      </div>

      {/* Confirm Password */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Confirm Password
          <input
            type="password"
            {...register("passwordConfirm")}
            style={{ display: "block", width: "100%", marginTop: 4 }}
          />
        </label>

        {/* ⭐ Zod/RHF error for passwordConfirm
            - 길이 검증 실패
            - refine에서 "Passwords do not match" 도 여기로 옴 */}
        {errors.passwordConfirm && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.passwordConfirm.message as string}
          </div>
        )}
      </div>
    </>
  );
}