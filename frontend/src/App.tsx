// src/App.tsx

import { AppRoutes } from "@/routes/AppRoutes";
import { useAuthBootstrap } from "@/features/auth/hooks/useAuthBootstrap";
import { useAuthStore } from "@/store/useAuthStore";
import { AuthDebugPanel } from "@/shared/components/AuthDebugPanel";

function App() {
  useAuthBootstrap();

  const phase = useAuthStore((s) => s.phase);

  if (phase === "bootstrapping") {
    return <div>Loading...</div>;
  }

  return (
    <div style={{ padding: 20 }}>
      <AuthDebugPanel />
      <hr />
      <AppRoutes />
    </div>
  );
}

export default App;