// src/features/auth/register/forms/hooks/useSignupProfileForm.ts
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import {
  signupProfileSchema,
  type SignupProfileFormInput,
} from "../schemas/signupProfile.schema";

export function useSignupProfileForm(defaults?: Partial<SignupProfileFormInput>) {
  return useForm<SignupProfileFormInput>({
    resolver: zodResolver(signupProfileSchema),
    defaultValues: {
      name: defaults?.name ?? "",
      gender: defaults?.gender ?? "", // ✅ allowed in UI
      birthDate: defaults?.birthDate ?? "",
    },
    mode: "onChange",
  });
}