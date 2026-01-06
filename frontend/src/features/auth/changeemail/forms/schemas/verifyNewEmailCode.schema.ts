import { z } from "zod";

export const verifyNewEmailCodeSchema = z.object({
  code: z
    .string()
    .min(6, "Please enter the 6-digit code.")
    .max(6, "Please enter the 6-digit code.")
    .regex(/^\d+$/, "Please enter a 6-digit numeric code."),
});

export type VerifyNewEmailCodeFormValues = z.infer<typeof verifyNewEmailCodeSchema>;