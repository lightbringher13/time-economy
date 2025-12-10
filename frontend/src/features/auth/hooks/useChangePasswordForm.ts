// features/auth/hooks/useChangePasswordForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { changePasswordSchema, type ChangePasswordFormValues } from "../types/changePasswordForm";


export function useChangePasswordForm() {
  return useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmNewPassword: "",
    },
    mode: "onChange", // 또는 "onBlur" 취향대로
  });
}