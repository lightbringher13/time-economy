// src/features/health/api/healthApi.ts
import { apiClient } from "@/shared/api/apiClient";
import type { HealthResponse } from "../types/HealthResponse";

/**
 * Simple health check.
 * Calls GET /api/health and returns health payload.
 */
export async function getHealthApi(): Promise<HealthResponse> {
  const res = await apiClient.get<HealthResponse>("/health");
  return res.data; // <-- Axios returns { data, status, headers ... }
}