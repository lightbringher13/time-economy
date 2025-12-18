import { apiClient } from "@/shared/api/apiClient";
import type {
  CreateOtpRequest,
  CreateOtpResponse,
  VerifyOtpRequest,
  VerifyOtpResponse,
  CreateLinkRequest,
  CreateLinkResponse,
  VerifyLinkRequest,
  VerifyLinkResponse,
} from "../types/verificationTypes";

const BASE = "/auth/public/verification";

export async function createOtpApi(req: CreateOtpRequest) {
  const { data } = await apiClient.post<CreateOtpResponse>(`${BASE}/otp`, req);
  return data;
}

export async function verifyOtpApi(req: VerifyOtpRequest) {
  const { data } = await apiClient.post<VerifyOtpResponse>(`${BASE}/otp/verify`, req);
  return data;
}

export async function createLinkApi(req: CreateLinkRequest) {
  const { data } = await apiClient.post<CreateLinkResponse>(`${BASE}/link`, req);
  return data;
}

export async function verifyLinkApi(req: VerifyLinkRequest) {
  const { data } = await apiClient.post<VerifyLinkResponse>(`${BASE}/link/verify`, req);
  return data;
}