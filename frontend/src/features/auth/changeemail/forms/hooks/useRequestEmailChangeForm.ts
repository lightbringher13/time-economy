// src/features/auth/change-email/hooks/useRequestEmailChangeForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  requestEmailChangeSchema,
  type RequestEmailChangeFormValues,
} from "../schemas/requestEmailChange.schema.ts";

export function useRequestEmailChangeForm() {
  return useForm<RequestEmailChangeFormValues>({
    resolver: zodResolver(requestEmailChangeSchema),
    defaultValues: {
      currentPassword: "",
      newEmail: "",
    },
    mode: "onSubmit",     // or "onChange" if you want live validation
    reValidateMode: "onChange",
  });
}