import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import {
  signupEmailSchema,
  type SignupEmailFormValues,
} from "../schemas/signupEmail.schema";

export function useSignupEmailForm(defaultEmail?: string) {
  return useForm<SignupEmailFormValues>({
    resolver: zodResolver(signupEmailSchema),
    defaultValues: {
      email: defaultEmail ?? "",
    },
    mode: "onChange",
  });
}