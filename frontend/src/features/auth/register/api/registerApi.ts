import { apiClient } from "@/shared/api/apiClient";

import type { RegisterRequestDto, RegisterResponseDto } from "./registerApi.types";

export async function registerApi(
  data: RegisterRequestDto
): Promise<RegisterResponseDto> {
  const res = await apiClient.post<RegisterResponseDto>("/auth/register", data);
  return res.data;
}