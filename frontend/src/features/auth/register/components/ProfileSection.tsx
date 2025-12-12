// features/auth/components/register/ProfileSection.tsx
import { useFormContext } from "react-hook-form";
import type { RegisterFormValues } from "../schemas/registerForm";

export function ProfileSection() {
  const {
    register,
    formState: { errors },
  } = useFormContext<RegisterFormValues>();

  return (
    <>
      {/* Name */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Name
          <input
            type="text"
            {...register("name")}
            style={{ display: "block", width: "100%", marginTop: 4 }}
          />
        </label>
        {errors.name && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.name.message as string}
          </div>
        )}
      </div>

      {/* Gender */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Gender
          <select
            {...register("gender")}
            style={{ display: "block", width: "100%", marginTop: 4 }}
          >
            <option value="">Select gender</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
        </label>
        {errors.gender && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.gender.message as string}
          </div>
        )}
      </div>

      {/* Birth Date */}
      <div style={{ marginBottom: 12 }}>
        <label>
          Birth Date
          <input
            type="date"
            {...register("birthDate")}
            style={{ display: "block", width: "100%", marginTop: 4 }}
          />
        </label>
        {errors.birthDate && (
          <div style={{ color: "red", marginTop: 4 }}>
            {errors.birthDate.message as string}
          </div>
        )}
      </div>
    </>
  );
}