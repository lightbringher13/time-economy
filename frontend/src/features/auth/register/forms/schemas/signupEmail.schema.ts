import { z } from "zod";

export const signupEmailSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, "Email is required.")
    .email("Please enter a valid email address."),
});

export type SignupEmailFormValues = z.infer<typeof signupEmailSchema>;

export const signupEmailOtpSchema = z.object({
  code: z
    .string()
    .trim()
    .length(6, "Enter the 6-digit code.")
    .regex(/^\d{6}$/, "Enter digits only (6 digits)."),
});

export type SignupEmailOtpFormValues = z.infer<typeof signupEmailOtpSchema>;