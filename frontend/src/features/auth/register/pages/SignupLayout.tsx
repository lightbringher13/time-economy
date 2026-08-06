import { useEffect, useRef } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { SignupFlowProvider } from "../hooks/SignupFlowContext";
import { SignupShellUiProvider } from "../hooks/SignupShellUiContext";
import { SignupOtpUiProvider } from "../hooks/SignupOtpUiContext";
import { useSignupFlow } from "../hooks/useSignupFlow";
import { SignupProgressCard } from "../components/ui/SignupProgressCard";

import { signupPathFromState } from "../routes/signupPathFromState";
import { allowedPathsByState } from "../routes/signupAllowedPaths";
import type { SignupSessionState } from "../api/signupApi.types";

export default function SignupLayout() {
  return (
    <SignupFlowProvider>
      <SignupShellUiProvider>
        <SignupOtpUiProvider>
          <SignupLayoutInner />
        </SignupOtpUiProvider>
      </SignupShellUiProvider>
    </SignupFlowProvider>
  );
}

function SignupLayoutInner() {
  const flow = useSignupFlow();
  const navigate = useNavigate();
  const location = useLocation();

  const state: SignupSessionState = flow.view.state;
  const path = location.pathname;

  // ✅ first time entering signup tree (mount) => always go canonical
  const didInitialRedirect = useRef(false);

  useEffect(() => {
    // wait for status
    if (flow.statusQuery.isLoading || flow.statusQuery.isFetching) return;

    // hard failure
    if (flow.statusQuery.isError) {
      navigate(ROUTES.LOGIN, { replace: true });
      return;
    }

    const canonical = signupPathFromState(state);

    // ✅ first mount rule: always land on canonical for current state
    if (!didInitialRedirect.current) {
      didInitialRedirect.current = true;

      if (path !== canonical) {
        navigate(canonical, { replace: true });
      }
      return; // important: skip allowed-path logic on first mount
    }

    const allowed = allowedPathsByState[state];

    // ✅ allow back/forward / edits if path is allowed for this state
    if (allowed.has(path)) return;

    // otherwise redirect to canonical for this state
    navigate(canonical, { replace: true });
  }, [
    flow.statusQuery.isLoading,
    flow.statusQuery.isFetching,
    flow.statusQuery.isError,
    state,
    path,
    navigate,
  ]);

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupProgressCard />
      <Outlet />
    </div>
  );
}
