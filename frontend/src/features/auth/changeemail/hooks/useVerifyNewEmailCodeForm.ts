// src/features/auth/change-email/hooks/useVerifyNewEmailCodeForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  verifyNewEmailCodeSchema,
  type VerifyNewEmailCodeFormValues,
} from "../schemas/changeEmailSchemas";

export function useVerifyNewEmailCodeForm() {
  return useForm<VerifyNewEmailCodeFormValues>({
    resolver: zodResolver(verifyNewEmailCodeSchema),
    defaultValues: {
      code: "",
    },
    mode: "onSubmit",
    reValidateMode: "onChange",
  });
}