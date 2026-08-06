// src/features/auth/register/queries/extractApiMessage.ts
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

export function extractApiMessage(err: unknown, fallback: string) {
  // Axios-like shape
  const response = isRecord(err) ? err.response : undefined;
  const data = isRecord(response) ? response.data : undefined;

  const msg =
    (isRecord(data) ? data.message : undefined) ??
    (isRecord(data) ? data.error : undefined) ??
    (isRecord(err) ? err.message : undefined);

  if (typeof msg === "string" && msg.trim().length > 0) return msg;
  return fallback;
}
