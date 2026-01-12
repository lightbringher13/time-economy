// src/features/auth/register/queries/extractApiMessage.ts
export function extractApiMessage(err: unknown, fallback: string) {
  // Axios-like shape
  const anyErr = err as any;

  const msg =
    anyErr?.response?.data?.message ??
    anyErr?.response?.data?.error ??
    anyErr?.message;

  if (typeof msg === "string" && msg.trim().length > 0) return msg;
  return fallback;
}