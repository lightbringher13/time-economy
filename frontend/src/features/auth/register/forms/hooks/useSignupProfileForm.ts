import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import {
  signupProfileSchema,
  type SignupProfileFormValues,
  type SignupGender,
} from "../schemas/signupProfile.schema";

export function useSignupProfileForm(defaults?: Partial<SignupProfileFormValues>) {
  return useForm<SignupProfileFormValues>({
    resolver: zodResolver(signupProfileSchema),
    defaultValues: {
      name: defaults?.name ?? "",
      gender: (defaults?.gender ?? "OTHER") as SignupGender,
      birthDate: defaults?.birthDate ?? "", // YYYY-MM-DD
    },
    mode: "onChange",
  });
}