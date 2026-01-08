import { z } from "zod";

export const signupPhoneSchema = z.object({
  phoneNumber: z
    .string()
    .trim()
    .min(1, "Phone number is required.")
    // keep it flexible; BE can normalize later (E.164)
    .regex(/^[0-9+\-\s()]{7,20}$/, "Please enter a valid phone number."),
});

export type SignupPhoneFormValues = z.infer<typeof signupPhoneSchema>;

export const signupPhoneOtpSchema = z.object({
  code: z
    .string()
    .trim()
    .length(6, "Enter the 6-digit code.")
    .regex(/^\d{6}$/, "Enter digits only (6 digits)."),
});

export type SignupPhoneOtpFormValues = z.infer<typeof signupPhoneOtpSchema>;