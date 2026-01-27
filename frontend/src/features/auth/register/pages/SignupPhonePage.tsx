// src/features/auth/register/pages/SignupPhonePage.tsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { SignupPhoneStep } from "../components/forms/SignupPhoneStep";
import { useSignupFlow } from "../hooks/SignupFlowContext";
import { useSignupShellUi } from "../hooks/SignupShellUiContext";
import { useSignupOtpUiContext } from "../hooks/SignupOtpUiContext";

import { useSignupPhoneForm } from "../forms/hooks/useSignupPhoneForm";
import { ROUTES } from "@/routes/paths";
import { signupPathFromState } from "../routes/signupPathFromState";

import { getApiError } from "@/shared/api/apiError";

export default function SignupPhonePage() {
  const navigate = useNavigate();
  const flow = useSignupFlow();
  const shellUi = useSignupShellUi();
  const otpUi = useSignupOtpUiContext();

  const phoneForm = useSignupPhoneForm();

  // Prefill from server, but never overwrite user edits
  useEffect(() => {
    const serverPhone = flow.view.phoneNumber;
    const dirty = phoneForm.formState.dirtyFields;

    if (serverPhone && !dirty.phoneNumber) {
      phoneForm.setValue("phoneNumber", serverPhone, { shouldValidate: true });
    }
  }, [flow.view.phoneNumber, phoneForm, phoneForm.formState.dirtyFields]);

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

  const clearUiErrors = () => {
    phoneForm.clearErrors("phoneNumber");
    shellUi.clearBanner();
  };

  const onSend = async () => {
    clearUiErrors();

    const ok = await phoneForm.trigger("phoneNumber");
    if (!ok) return;

    const phone = phoneForm.getValues("phoneNumber");

    try {
      const res = await flow.sendPhoneOtp(phone);

      // ✅ seed cooldown timer from server (works for SENT + THROTTLED)
      otpUi.applySendResult("PHONE", res);

      // ✅ navigate only when accepted/sent or already verified
      if (res.outcome === "SENT" || res.outcome === "ALREADY_VERIFIED") {
        navigate(signupPathFromState(res.state));
        return;
      }

      // ✅ throttled is not an error UX
      if (res.outcome === "THROTTLED") {
        shellUi.showBanner({
          kind: "info",
          code: "THROTTLED",
          message:
            res.retryAfterSeconds != null
              ? `Please wait ${res.retryAfterSeconds}s before resending.`
              : "Please wait a moment before resending.",
        });
        return;
      }

      // ✅ delivery failed etc.
      if (res.outcome === "DELIVERY_FAILED") {
        shellUi.showBanner({
          kind: "error",
          code: res.outcome,
          message: "We couldn't send the code right now. Please try again.",
        });
        return;
      }
    } catch (err) {
      const apiErr = getApiError(err);

      // ✅ inline field errors
      if (apiErr?.code === "INVALID_DESTINATION" || apiErr?.code === "PHONE_ALREADY_USED") {
        phoneForm.setError("phoneNumber", { type: "server", message: apiErr.message });
        return;
      }

      // ✅ banner errors (invalid state / missing session / network / unknown)
      shellUi.showBanner({
        kind: "error",
        message: apiErr?.message ?? "Request failed.",
        code: apiErr?.code,
      });

      // Optional: if session is gone, route user out
      // if (apiErr?.code === "NO_SESSION") navigate(ROUTES.LOGIN, { replace: true });
    }
  };

  return (
    <div style={{ maxWidth: 460, margin: "0 auto", padding: 16 }}>
      <SignupPhoneStep
        title="Create your account"
        subtitle="Step 2 — Verify phone"
        phoneVerified={Boolean(flow.view.phoneVerified)}
        maskedPhone={flow.view.phoneNumber}
        // ✅ banner handles non-field errors
        error={undefined}
        phoneForm={phoneForm}
        onSend={onSend}
        onCancel={onCancel}
        sending={Boolean(flow.loading?.sendOtp)}
        cancelling={Boolean(flow.loading?.cancel)}
        phoneDisabled={false}
        sendLabel="Send code"
        cancelLabel="Cancel"
      />
    </div>
  );
}