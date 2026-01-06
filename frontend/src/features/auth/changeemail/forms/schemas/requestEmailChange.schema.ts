import { z } from "zod";

export const requestEmailChangeSchema = z.object({
  currentPassword: z
    .string()
    .min(1, "Please enter your current password."),

  newEmail: z
    .string()
    .min(1, "Please enter your new email address.")
    .email("Please enter a valid email address."),
});

export type RequestEmailChangeFormValues = z.infer<typeof requestEmailChangeSchema>;