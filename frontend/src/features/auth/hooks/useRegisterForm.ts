// features/auth/hooks/useRegisterForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { registerSchema } from "../types/registerForm";
import type { RegisterFormValues } from "../types/auth";

export function useRegisterForm() {
  return useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: "",
      emailCode: "",
      password: "",
      passwordConfirm: "",
      phoneNumber: "",
      phoneCode: "",
      name: "",
      gender: "",       // <- matches schema union
      birthDate: "",
    },
    mode: "onChange",   // or "onBlur" if you like
  });
}