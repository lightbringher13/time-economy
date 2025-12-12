// features/auth/hooks/useForgotPasswordForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { type ForgotPasswordFormValues, forgotPasswordSchema } from "../schemas/forgotPasswordForm";

export function useForgotPasswordForm() {
  return useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: "",
    },
    mode: "onBlur", // onChange/onSubmit 아무거나 취향대로
  });
}