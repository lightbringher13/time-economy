// src/features/auth/register/hooks/extractApiMessage.ts
export function extractApiMessage(err: any, fallback: string) {
  return (
    err?.response?.data?.message ||
    err?.response?.data?.error ||
    err?.message ||
    fallback
  );
}