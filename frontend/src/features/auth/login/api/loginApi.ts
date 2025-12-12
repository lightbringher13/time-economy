import { apiClient } from "@/shared/api/apiClient";

import type { LoginRequest,LoginResponse } from "./loginApi.types";


export async function loginApi(data: LoginRequest): Promise<LoginResponse> {
  const res = await apiClient.post<LoginResponse>("/auth/login", data);
  return res.data;
}