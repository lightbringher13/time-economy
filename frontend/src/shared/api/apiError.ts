import axios, { AxiosError } from "axios";

export type ApiErrorResponse = {
  code: string;     // e.g. "EMAIL_ALREADY_USED"
  message: string;  // human message
};

export function getApiError(err: unknown): ApiErrorResponse | null {
  if (!axios.isAxiosError(err)) return null;

  const data = (err as AxiosError<any>).response?.data;
  if (!data || typeof data !== "object") return null;

  if (typeof data.code === "string" && typeof data.message === "string") {
    return data as ApiErrorResponse;
  }
  return null;
}