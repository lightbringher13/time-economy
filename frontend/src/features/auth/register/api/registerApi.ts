import { apiClient } from "@/shared/api/apiClient";

import type { RegisterRequest, RegisterResponse } from "./registerApi.types";

export async function registerApi(
  data: RegisterRequest
): Promise<RegisterResponse> {
  const res = await apiClient.post<RegisterResponse>("/auth/register", data);
  return res.data;
}