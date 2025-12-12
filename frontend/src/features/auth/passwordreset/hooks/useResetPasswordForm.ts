// features/auth/hooks/useResetPasswordForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { type ResetPasswordFormValues, resetPasswordSchema } from "../schemas/resetPasswordForm";

export function useResetPasswordForm() {
  return useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      password: "",
      passwordConfirm: "",
    },
    mode: "onChange",
  });
}