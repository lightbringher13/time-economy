// src/features/auth/register/pages/SignupLayout.tsx
import { useEffect } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { SignupFlowProvider, useSignupFlow } from "../hooks/SignupFlowContext";
import { SignupShellUiProvider } from "../hooks/SignupShellUiContext";
import { signupPathFromState } from "../routes/signupRouteMap";

const EXEMPT_PATHS = new Set<string>([
  ROUTES.SIGNUP_EMAIL_EDIT,
  ROUTES.SIGNUP_PHONE_EDIT,
]);

export default function SignupLayout() {
  return (
    <SignupFlowProvider>
      <SignupShellUiProvider>
        <SignupLayoutInner />
      </SignupShellUiProvider>
    </SignupFlowProvider>
  );
}

function SignupLayoutInner() {
  const flow = useSignupFlow();
  const navigate = useNavigate();
  const location = useLocation();

  const state = flow.view.state ?? flow.state;

  useEffect(() => {
    if (EXEMPT_PATHS.has(location.pathname)) return;

    // wait for status to settle
    if (flow.statusQuery.isLoading || flow.statusQuery.isFetching) return;

    // hard failure => exit signup
    if (flow.statusQuery.isError) {
      navigate(ROUTES.LOGIN, { replace: true });
      return;
    }

    // allow profile revisit from review state
    if (location.pathname === ROUTES.SIGNUP_PROFILE && state === "PROFILE_READY") {
      return;
    }

    // canonical route
    const expected = signupPathFromState(state);
    if (expected !== location.pathname) {
      navigate(expected, { replace: true });
    }
  }, [
    flow.statusQuery.isLoading,
    flow.statusQuery.isFetching,
    flow.statusQuery.isError,
    state,
    location.pathname,
    navigate,
  ]);

  return <Outlet />;
}