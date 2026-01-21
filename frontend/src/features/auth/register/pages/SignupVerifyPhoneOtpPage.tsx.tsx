// src/features/auth/register/pages/SignupVerifyPhoneOtpPage.tsx
import { useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

import { useSignupFlow } from "../hooks/SignupFlowContext";
import { useSignupShellUi } from "../hooks/SignupShellUiContext";
import { signupPathFromState } from "../routes/signupPathFromState";

import { useSignupPhoneOtpForm } from "../forms/hooks/useSignupPhoneOtpForm";
import { SignupVerifyOtpStep } from "../components/forms/SignupVerifyOtpStep";

import { getApiError } from "@/shared/api/apiError";

export default function SignupVerifyPhoneOtpPage() {
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi();
  const navigate = useNavigate();

  const form = useSignupPhoneOtpForm();

  const clearUiErrors = useCallback(() => {
    shellUi.clearBanner();
    form.clearErrors();
  }, [shellUi, form]);

  // always start clean
  useEffect(() => {
    form.reset({ code: "" }, { keepDirty: false, keepTouched: false });
    shellUi.clearBanner();
  }, [form, shellUi]);

  const onCancel = () => {
    shellUi.openCancelModal({
      reason: "user",
      title: "Cancel signup?",
      description: "Your signup progress will be discarded. You can start again anytime.",
      confirmLabel: "Cancel signup",
      cancelLabel: "Keep going",
      destructive: true,
      onConfirm: async () => {
        shellUi.clearBanner();
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
  };

  const onVerify = async (code: string) => {
    clearUiErrors();

    try {
      const res = await flow.verifyPhoneOtp(code); // ✅ resolves only on VERIFIED (2xx)
      form.reset({ code: "" }, { keepDirty: false, keepTouched: false });
      navigate(signupPathFromState(res.state));
    } catch (err) {
      const apiErr = getApiError(err);

      // ✅ inline (field) errors
      if (apiErr?.code === "WRONG_CODE") {
        form.setError("code", {
          type: "server",
          message: apiErr.message ?? "Incorrect code. Please try again.",
        });
        return;
      }

      if (apiErr?.code === "INVALID_INPUT") {
        form.setError("code", {
          type: "server",
          message: apiErr.message ?? "Invalid code format.",
        });
        return;
      }

      // ✅ banner errors (session/state/network/unknown)
      shellUi.showBanner({
        kind: "error",
        code: apiErr?.code,
        message: apiErr?.message ?? "Verification failed. Please try again.",
      });

      // Optional: if session is gone, route out (recommended UX)
      // if (apiErr?.code === "NO_SESSION") {
      //   navigate(ROUTES.LOGIN, { replace: true });
      // }
    }
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupVerifyOtpStep
        title="Verify your phone"
        subtitle="Enter the 6-digit code we sent via SMS."
        destinationLabel="Phone"
        maskedDestination={flow.view.phoneNumber ?? null}
        error={undefined} // banner is global via shellUi
        form={form as any}
        onVerify={onVerify}
        onCancel={onCancel}
        verifying={Boolean(flow.loading?.verifyOtp)}
        cancelling={Boolean(flow.loading?.cancel)}
      />
    </div>
  );
}