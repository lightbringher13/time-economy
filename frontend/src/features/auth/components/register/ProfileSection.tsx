// features/auth/components/register/ProfileSection.tsx
import { useFormContext } from "react-hook-form";
import type { RegisterFormValues } from "../../types/auth";

export function ProfileSection() {
  const { register } = useFormContext<RegisterFormValues>();

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
      </div>
    </>
  );
}