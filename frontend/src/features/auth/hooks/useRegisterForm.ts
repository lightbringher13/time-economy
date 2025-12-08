// features/auth/hooks/useRegisterForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { registerSchema } from "../types/registerForm";
import type { RegisterFormValues } from "../types/registerForm";

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
      gender: "",       // <- schema에서 허용하는 값 중 하나
      birthDate: "",
    },
    mode: "onChange",
  });
}