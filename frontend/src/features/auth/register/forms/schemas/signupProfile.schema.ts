// src/features/auth/register/forms/schemas/signupProfile.schema.ts
import { z } from "zod";

export const genderEnum = z.enum(["MALE", "FEMALE"]);
export type SignupGender = z.infer<typeof genderEnum>;

/**
 * ✅ UI input can be "" (placeholder from <select>),
 * but after validation it becomes SignupGender.
 */
export const genderSchema = z
  .union([z.literal(""), genderEnum])
  .refine((v) => v !== "", { message: "Gender is required." })
  .transform((v) => v as SignupGender);

export const signupProfileSchema = z.object({
  name: z.string().trim().min(1, "Name is required.").max(50, "Name is too long."),
  gender: genderSchema,
  birthDate: z
    .string()
    .trim()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Birth date must be YYYY-MM-DD."),
});

/**
 * ✅ Big-co pattern:
 * - Input type = what the form can temporarily hold (includes "")
 * - Output type = what you can safely send to backend (no "")
 */
export type SignupProfileFormInput = z.input<typeof signupProfileSchema>;
export type SignupProfileFormValues = z.output<typeof signupProfileSchema>;