import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import {
  signupPhoneOtpSchema,
  type SignupPhoneOtpFormValues,
} from "../schemas/signupPhone.schema";

export function useSignupPhoneOtpForm() {
  return useForm<SignupPhoneOtpFormValues>({
    resolver: zodResolver(signupPhoneOtpSchema),
    defaultValues: {
      code: "",
    },
    mode: "onChange",
  });
}