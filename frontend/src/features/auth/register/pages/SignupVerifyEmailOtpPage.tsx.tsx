// src/features/auth/register/pages/SignupVerifyEmailOtpPage.tsx
import { useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { useSignupFlow } from "../hooks/useSignupFlow";
import { useSignupShellUi } from "../hooks/useSignupShellUi";
import { useSignupOtpUiContext } from "../hooks/useSignupOtpUiContext";

import { signupPathFromState } from "../routes/signupPathFromState";

import { useSignupEmailOtpForm } from "../forms/hooks/useSignupEmailOtpForm";
import { SignupVerifyOtpStep } from "../components/forms/SignupVerifyOtpStep";

import { getApiError } from "@/shared/api/apiError";

export default function SignupVerifyEmailOtpPage() {
  const flow = useSignupFlow();
  const navigate = useNavigate();

  const form = useSignupEmailOtpForm();
  const { clearErrors, reset, setError } = form;

  // ✅ destructure stable functions (do NOT depend on whole objects that change)
  const { clearBanner, showBanner, openCancelModal } = useSignupShellUi();
  const { applyVerifyResult, applyVerifyError, clearVerifyFeedback } = useSignupOtpUiContext();

  const clearUiErrors = useCallback(() => {
    clearBanner();
    clearErrors();
    clearVerifyFeedback("EMAIL");
  }, [clearBanner, clearErrors, clearVerifyFeedback]);

  // ✅ run once on mount (only stable deps)
  useEffect(() => {
    reset({ code: "" }, { keepDirty: false, keepTouched: false });
    clearBanner();
    clearVerifyFeedback("EMAIL");
  }, [reset, clearBanner, clearVerifyFeedback]);

  const onCancel = () => {
    openCancelModal({
      reason: "user",
      title: "Cancel signup?",
      description: "Your signup progress will be discarded. You can start again anytime.",
      confirmLabel: "Cancel signup",
      cancelLabel: "Keep going",
      destructive: true,
      onConfirm: async () => {
        clearBanner();
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
  };

  const onVerify = async (code: string) => {
    clearUiErrors();

    try {
      const res = await flow.verifyEmailOtp(code);
      applyVerifyResult("EMAIL", res);

      reset({ code: "" }, { keepDirty: false, keepTouched: false });
      navigate(signupPathFromState(res.state));
    } catch (err) {
      const apiErr = getApiError(err);
      applyVerifyError("EMAIL", apiErr);

      if (apiErr?.code === "NO_SESSION") {
        showBanner({
          kind: "error",
          code: apiErr.code,
          message: apiErr.message ?? "Session expired. Please start again.",
        });
        navigate(ROUTES.LOGIN, { replace: true });
        return;
      }

      if (apiErr?.code === "WRONG_CODE") {
        const attempts = apiErr.details?.remainingAttempts;
        setError("code", {
          type: "server",
          message:
            attempts != null
              ? `${apiErr.message ?? "Incorrect code."} (${attempts} attempts left)`
              : apiErr.message ?? "Incorrect code. Please try again.",
        });
        return;
      }

      if (apiErr?.code === "INVALID_INPUT") {
        setError("code", {
          type: "server",
          message: apiErr.message ?? "Invalid code format.",
        });
        return;
      }

      if (apiErr?.code === "THROTTLED") {
        const retry = apiErr.details?.retryAfterSeconds;
        showBanner({
          kind: "error",
          code: apiErr.code,
          message: retry != null ? `Too many requests. Try again in ${retry}s.` : (apiErr.message ?? "Too many requests."),
        });
        return;
      }

      // LOCKED / EXPIRED / NO_PENDING_OTP / fallback
      showBanner({
        kind: "error",
        code: apiErr?.code,
        message: apiErr?.message ?? "Verification failed. Please try again.",
      });
    }
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupVerifyOtpStep
        title="Verify your email"
        subtitle="Enter the 6-digit code we sent to your email."
        destinationLabel="Email"
        maskedDestination={flow.view.email ?? null}
        error={undefined}
        form={form}
        onVerify={onVerify}
        onCancel={onCancel}
        verifying={Boolean(flow.loading?.verifyOtp)}
        cancelling={Boolean(flow.loading?.cancel)}
      />
    </div>
  );
}
