import { z } from "zod";

const genderSchema = z.enum(["MALE", "FEMALE", "OTHER"]);

export const signupProfileSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Name is required.")
    .max(50, "Name is too long."),

  gender: genderSchema,

  // store as YYYY-MM-DD string in form, convert to LocalDate at BE
  birthDate: z
    .string()
    .trim()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Birth date must be YYYY-MM-DD."),
});

export type SignupProfileFormValues = z.infer<typeof signupProfileSchema>;
export type SignupGender = z.infer<typeof genderSchema>;