import { z } from "zod";

const genderValues = ["", "MALE", "FEMALE", "OTHER"] as const;

export const registerSchema = z
  .object({
    email: z
      .string()
      .min(1, "Email is required")
      .email("Invalid email address"),

    emailCode: z.string().min(1, "Email verification code is required"),

    password: z.string().min(8, "Password must be at least 8 characters"),
    passwordConfirm: z.string().min(8, "Password confirmation is required"),

    phoneNumber: z.string().min(1, "Phone number is required"),
    phoneCode: z.string().min(1, "Phone verification code is required"),

    name: z.string().min(1, "Name is required"),

    gender: z
      .enum(genderValues)                           // no second argument
      .refine((v) => v !== "", {
        message: "Gender is required",
      }),

    birthDate: z.string().min(1, "Birth date is required"),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: "Passwords do not match",
    path: ["passwordConfirm"],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;