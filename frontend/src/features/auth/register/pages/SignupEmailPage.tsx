// src/features/auth/register/pages/SignupEmailPage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { SignupEmailStep } from "../components/forms/SignupEmailStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";
import { useSignupShellUi } from "../hooks/SignupShellUiContext";

import { useSignupEmailForm } from "../forms/hooks/useSignupEmailForm";
import { ROUTES } from "@/routes/paths";
import { signupPathFromState } from "../routes/signupPathFromState";

import { getApiError } from "@/shared/api/apiError";

export default function SignupEmailPage() {
  const navigate = useNavigate();
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi();
  const emailForm = useSignupEmailForm();

  // Prefill from server, but never overwrite user edits
  useEffect(() => {
    const serverEmail = flow.view.email;
    const dirty = emailForm.formState.dirtyFields;

    if (serverEmail && !dirty.email) {
      emailForm.setValue("email", serverEmail, { shouldValidate: true });
    }
  }, [flow.view.email, emailForm, emailForm.formState.dirtyFields]);

  const onCancel = () => {
    shellUi.openCancelModal({
      reason: "user",
      title: "Cancel signup?",
      description: "Your signup progress will be discarded. You can start again anytime.",
      confirmLabel: "Cancel signup",
      cancelLabel: "Keep going",
      destructive: true,
      onConfirm: async () => {
        navigate(ROUTES.LOGIN, { replace: true });
        await flow.cancel();
      },
    });
  };

  const clearUiErrors = () => {
    emailForm.clearErrors("email");
    shellUi.clearBanner(); // ✅ implement in SignupShellUiContext (no-op if none)
  };

  const onSend = async () => {
    clearUiErrors();

    const ok = await emailForm.trigger("email");
    if (!ok) return;

    const email = emailForm.getValues("email");

    try {
      const res = await flow.sendEmailOtp(email);

      // Navigate based on BE-returned state (EMAIL_OTP_SENT => verify page)
      navigate(signupPathFromState(res.state));
    } catch (err) {
      const apiErr = getApiError(err);

      // ✅ inline field errors
      if (apiErr?.code === "INVALID_DESTINATION" || apiErr?.code === "EMAIL_ALREADY_USED") {
        emailForm.setError("email", { type: "server", message: apiErr.message });
        return;
      }

      // ✅ banner errors (unknown / network / invalid state, etc.)
      shellUi.showBanner({
        kind: "error",
        message: apiErr?.message ?? "Request failed.",
        code: apiErr?.code,
      });
    }
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupEmailStep
        title="Create your account"
        subtitle="Step 1 — Verify email"
        emailVerified={Boolean(flow.view.emailVerified)}
        maskedEmail={flow.view.email}
        error={undefined}
        emailForm={emailForm}
        onSend={onSend}
        onCancel={onCancel}
        sending={Boolean(flow.loading?.sendOtp)}
        cancelling={Boolean(flow.loading?.cancel)}
        emailDisabled={false}
        sendLabel="Send code"
        cancelLabel="Cancel"
      />
    </div>
  );
}