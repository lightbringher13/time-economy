import { z } from "zod";

export const verifySecondFactorSchema = z.object({
  code: z
    .string()
    .min(6, "Please enter the 6-digit code.")
    .max(6, "Please enter the 6-digit code.")
    .regex(/^\d+$/, "Please enter a 6-digit numeric code."),
});

export type VerifySecondFactorFormValues = z.infer<typeof verifySecondFactorSchema>;