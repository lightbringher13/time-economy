import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import {
  signupEmailOtpSchema,
  type SignupEmailOtpFormValues,
} from "../schemas/signupEmail.schema";

export function useSignupEmailOtpForm() {
  return useForm<SignupEmailOtpFormValues>({
    resolver: zodResolver(signupEmailOtpSchema),
    defaultValues: {
      code: "",
    },
    mode: "onChange",
  });
}