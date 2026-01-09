// src/features/auth/register/forms/hooks/useSignupPasswordForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import {
  signupPasswordSchema,
  type SignupPasswordFormValues,
} from "../schemas/signupPassword.schema";

export function useSignupPasswordForm() {
  return useForm<SignupPasswordFormValues>({
    resolver: zodResolver(signupPasswordSchema),
    defaultValues: {
      password: "",
      passwordConfirm: "",
    },
    mode: "onChange",
  });
}