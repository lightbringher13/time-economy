import { useEffect } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { SignupFlowProvider, useSignupFlow } from "../hooks/SignupFlowContext.tsx";
import { signupPathFromState } from "../routes/signupRouteMap";

const EXEMPT_PATHS = new Set<string>([
  ROUTES.SIGNUP_EMAIL_EDIT,
  ROUTES.SIGNUP_PHONE_EDIT,
  ROUTES.SIGNUP_DONE,
]);

export default function SignupLayout() {
  return (
    <SignupFlowProvider>
      <SignupLayoutInner />
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

    // 1) wait for bootstrap query (cookie/session init)
    if (flow.bootstrapQ.isLoading) return;

    // if bootstrap failed, exit signup
    if (flow.bootstrapQ.isError) {
      navigate(ROUTES.LOGIN, { replace: true });
      return;
    }

    // 2) status query runs only after bootstrapQ.isSuccess (enabled)
    if (flow.statusQuery.isLoading || flow.statusQuery.isFetching) return;

    // 3) interpret no-session (after both settled)
    const noSession =
      flow.statusQuery.isError &&
      (flow.error ?? "").toLowerCase().includes("cookie not found");

    if (noSession || !state) {
      navigate(ROUTES.LOGIN, { replace: true });
      return;
    }

    if (location.pathname === ROUTES.SIGNUP_PROFILE && state === "PROFILE_READY") {
      return;
    }

    // 4) canonical route
    const expected = signupPathFromState(state);
    if (expected !== location.pathname) {
      navigate(expected, { replace: true });
    }
  }, [
    flow.bootstrapQ.isLoading,
    flow.bootstrapQ.isError,
    flow.statusQuery.isLoading,
    flow.statusQuery.isFetching,
    flow.statusQuery.isError,
    flow.error,
    state,
    location.pathname,
    navigate,
  ]);

  return <Outlet />;
}