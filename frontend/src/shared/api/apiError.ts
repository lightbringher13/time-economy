import axios, { AxiosError } from "axios";

export type ApiErrorDetails = {
  otpExpiresAt?: string | null;
  remainingAttempts?: number | null;
  retryAfterSeconds?: number | null;
};

export type ApiErrorResponse = {
  code: string;
  message: string;
  details?: ApiErrorDetails;
};

function isApiErrorDetails(x: any): x is ApiErrorDetails {
  if (!x || typeof x !== "object") return false;
  const ok1 = x.otpExpiresAt == null || typeof x.otpExpiresAt === "string";
  const ok2 = x.remainingAttempts == null || typeof x.remainingAttempts === "number";
  const ok3 = x.retryAfterSeconds == null || typeof x.retryAfterSeconds === "number";
  return ok1 && ok2 && ok3;
}

export function getApiError(err: unknown): ApiErrorResponse | null {
  if (!axios.isAxiosError(err)) return null;

  const data = (err as AxiosError<any>).response?.data;
  if (!data || typeof data !== "object") return null;

  if (typeof data.code !== "string" || typeof data.message !== "string") return null;

  const details = isApiErrorDetails((data as any).details) ? (data as any).details : undefined;

  return {
    code: data.code,
    message: data.message,
    ...(details ? { details } : {}),
  };
}