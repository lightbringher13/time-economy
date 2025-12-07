// features/auth/components/register/PasswordSection.tsx
import { useFormContext } from "react-hook-form";
import type { RegisterFormValues } from "../../types/auth";

export function PasswordSection() {
  const { register } = useFormContext<RegisterFormValues>();

  return (
    <>
      <div style={{ marginBottom: 12 }}>
        <label>
          Password
          <input
            type="password"
            {...register("password")}
            style={{ display: "block", width: "100%", marginTop: 4 }}
          />
        </label>
      </div>

      <div style={{ marginBottom: 12 }}>
        <label>
          Confirm Password
          <input
            type="password"
            {...register("passwordConfirm")}
            style={{ display: "block", width: "100%", marginTop: 4 }}
          />
        </label>
      </div>
    </>
  );
}