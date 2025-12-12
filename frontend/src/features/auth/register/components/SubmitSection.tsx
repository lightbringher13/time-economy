// features/auth/components/register/SubmitSection.tsx

type SubmitSectionProps = {
  error: string | null;
  loading: boolean;
};

export function SubmitSection({ error, loading }: SubmitSectionProps) {
  return (
    <>
      {error && (
        <div style={{ color: "red", marginBottom: 8 }}>{error}</div>
      )}

      <button type="submit" disabled={loading} style={{ width: "100%" }}>
        {loading ? "Registering..." : "Register"}
      </button>
    </>
  );
}