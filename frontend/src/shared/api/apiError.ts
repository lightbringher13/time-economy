import axios from "axios";

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

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isApiErrorDetails(value: unknown): value is ApiErrorDetails {
  if (!isRecord(value)) return false;

  const ok1 =
    value.otpExpiresAt == null || typeof value.otpExpiresAt === "string";
  const ok2 =
    value.remainingAttempts == null ||
    typeof value.remainingAttempts === "number";
  const ok3 =
    value.retryAfterSeconds == null ||
    typeof value.retryAfterSeconds === "number";

  return ok1 && ok2 && ok3;
}

export function getApiErrorMessage(err: unknown, fallback: string): string {
  const response = isRecord(err) ? err.response : undefined;
  const data = isRecord(response) ? response.data : undefined;
  const responseMessage = isRecord(data) ? data.message : undefined;
  const directMessage = isRecord(err) ? err.message : undefined;
  const message = responseMessage ?? directMessage;

  return typeof message === "string" ? message : fallback;
}

export function getApiError(err: unknown): ApiErrorResponse | null {
  if (!axios.isAxiosError<unknown>(err)) return null;

  const data = err.response?.data;
  if (!isRecord(data)) return null;

  if (typeof data.code !== "string" || typeof data.message !== "string") {
    return null;
  }

  const details = isApiErrorDetails(data.details) ? data.details : undefined;

  return {
    code: data.code,
    message: data.message,
    ...(details ? { details } : {}),
  };
}
