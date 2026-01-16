import { useEffect } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { SignupFlowProvider, useSignupFlow } from "../hooks/SignupFlowContext";
import { signupPathFromState } from "../routes/signupRouteMap";

const EXEMPT_PATHS = new Set<string>([
  ROUTES.SIGNUP_EMAIL_EDIT,
  ROUTES.SIGNUP_PHONE_EDIT,
  // ✅ if /signup/done is outside the layout tree, remove this line
  // ROUTES.SIGNUP_DONE,
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
  console.log("SignupLayout mounted");
  return () => console.log("SignupLayout unmounted");
}, []);

  useEffect(() => {
    if (EXEMPT_PATHS.has(location.pathname)) return;

    // 1) wait for status to settle (status is safe even with no cookie)
    if (flow.statusQuery.isLoading || flow.statusQuery.isFetching) return;

    // 2) if status query errored (network/server), exit signup
    if (flow.statusQuery.isError) {
      navigate(ROUTES.LOGIN, { replace: true });
      return;
    }

    // 3) special allow: revisit profile from review state
    if (location.pathname === ROUTES.SIGNUP_PROFILE && state === "PROFILE_READY") {
      return;
    }

    // 4) canonical route (DRAFT -> /signup/email, PROFILE_READY -> /signup/review, etc.)
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