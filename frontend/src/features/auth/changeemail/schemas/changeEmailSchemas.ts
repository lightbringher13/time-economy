// src/features/auth/change-email/schemas/changeEmailSchemas.ts
import { z } from "zod";

export const requestEmailChangeSchema = z.object({
  currentPassword: z
    .string()
    .min(1, "현재 비밀번호를 입력해 주세요."),

  newEmail: z
    .string()
    .min(1, "새 이메일을 입력해 주세요.")
    .email("올바른 이메일 형식이 아닙니다."),
});

export type RequestEmailChangeFormValues = z.infer<
  typeof requestEmailChangeSchema
>;

export const verifyNewEmailCodeSchema = z.object({
  code: z
    .string()
    .min(6, "6자리 코드를 입력해 주세요.")
    .max(6, "6자리 코드를 입력해 주세요.")
    .regex(/^\d+$/, "숫자 6자리를 입력해 주세요."),
});

export type VerifyNewEmailCodeFormValues = z.infer<
  typeof verifyNewEmailCodeSchema
>;

export const verifySecondFactorSchema = z.object({
  code: z
    .string()
    .min(6, "6자리 코드를 입력해 주세요.")
    .max(6, "6자리 코드를 입력해 주세요.")
    .regex(/^\d+$/, "숫자 6자리를 입력해 주세요."),
});

export type VerifySecondFactorFormValues = z.infer<
  typeof verifySecondFactorSchema
>;