import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import {
  signupPhoneSchema,
  type SignupPhoneFormValues,
} from "../schemas/signupPhone.schema";

export function useSignupPhoneForm(defaultPhoneNumber?: string) {
  return useForm<SignupPhoneFormValues>({
    resolver: zodResolver(signupPhoneSchema),
    defaultValues: {
      phoneNumber: defaultPhoneNumber ?? "",
    },
    mode: "onChange",
  });
}