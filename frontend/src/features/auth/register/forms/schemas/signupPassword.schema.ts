// src/features/auth/register/forms/schemas/signupPassword.schema.ts
import { z } from "zod";

/**
 * Password rules (adjust as you want):
 * - 8~64 chars
 * - at least 1 letter
 * - at least 1 number
 * - at least 1 special char
 */
export const signupPasswordSchema = z
  .object({
    password: z
      .string()
      .min(8, "Password must be at least 8 characters.")
      .max(64, "Password must be at most 64 characters.")
      .regex(/[A-Za-z]/, "Password must include at least one letter.")
      .regex(/[0-9]/, "Password must include at least one number.")
      .regex(/[^A-Za-z0-9]/, "Password must include at least one special character."),
    passwordConfirm: z.string().min(1, "Please confirm your password."),
  })
  .superRefine((val, ctx) => {
    if (val.password !== val.passwordConfirm) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["passwordConfirm"],
        message: "Passwords do not match.",
      });
    }
  });

export type SignupPasswordFormValues = z.infer<typeof signupPasswordSchema>;