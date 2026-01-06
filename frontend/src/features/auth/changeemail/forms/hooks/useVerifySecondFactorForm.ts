// src/features/auth/change-email/hooks/useVerifySecondFactorForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  verifySecondFactorSchema,
  type VerifySecondFactorFormValues,
} from "../schemas/verifySecondFactor.schema.ts";

export function useVerifySecondFactorForm() {
  return useForm<VerifySecondFactorFormValues>({
    resolver: zodResolver(verifySecondFactorSchema),
    defaultValues: {
      code: "",
    },
    mode: "onSubmit",
    reValidateMode: "onChange",
  });
}