// src/features/auth/change-email/pages/ChangeEmailPage.tsx
import React, { useEffect, useMemo, useState, useRef } from "react";

// flow (status + actions)
import { useChangeEmailFlow } from "../hooks/useChangeEmailFlow";

// form hooks
import { useRequestEmailChangeForm } from "../forms/hooks/useRequestEmailChangeForm";
import { useVerifyNewEmailCodeForm } from "../forms/hooks/useVerifyNewEmailCodeForm";
import { useVerifySecondFactorForm } from "../forms/hooks/useVerifySecondFactorForm";

// step components
import { ChangeEmailStepRequest } from "../components/ChangeEmailStepRequest";
import { ChangeEmailStepVerifyNew } from "../components/ChangeEmailStepVerifyNew";
import { ChangeEmailStepSecondFactor } from "../components/ChangeEmailStepSecondFactor";
import { ChangeEmailStepDone } from "../components/ChangeEmailStepDone";

function extractMessage(err: any, fallback: string) {
  return err?.response?.data?.message || err?.message || fallback;
}

export function ChangeEmailPage() {
  // 1) forms
  const step1Form = useRequestEmailChangeForm();
  const step2Form = useVerifyNewEmailCodeForm();
  const step3Form = useVerifySecondFactorForm();

  // 2) flow
  const flow = useChangeEmailFlow();

  // 3) page-level error (UI-friendly)
  const [uiError, setUiError] = useState<string | null>(null);

  // optional: clear error when step changes
  useEffect(() => {
    setUiError(null);
  }, [flow.uiStep]);

  // ------ submit handlers (forms -> flow actions) ------
  const onSubmitRequest: React.FormEventHandler<HTMLFormElement> =
    step1Form.handleSubmit(async (values) => {
      try {
        setUiError(null);
        await flow.submitRequest(values);
        // next step is driven by server status polling + uiStep mapping
      } catch (err: any) {
        setUiError(extractMessage(err, "Failed to request email change. Please try again."));
      }
    });

  const onSubmitVerifyNew: React.FormEventHandler<HTMLFormElement> =
    step2Form.handleSubmit(async (values) => {
      try {
        setUiError(null);
        await flow.submitVerifyNew(values.code);
      } catch (err: any) {
        setUiError(extractMessage(err, "The new email verification code is invalid or expired."));
      }
    });

  const onSubmitSecondFactor: React.FormEventHandler<HTMLFormElement> =
    step3Form.handleSubmit(async (values) => {
      try {
        setUiError(null);
        await flow.submitSecondFactor(values.code);
      } catch (err: any) {
        setUiError(extractMessage(err, "The second-factor code is invalid or expired."));
      }
    });

  // optional: reset (pure FE reset + re-sync from server)
  const onReset = async () => {
    setUiError(null);
    step1Form.reset();
    step2Form.reset();
    step3Form.reset();

    // If you want: ask server again for active request
    try {
      await flow.refresh(); // re-sync based on /active
    } catch {
      // ignore
    }
  };

  const loading = flow.loading;

  // Safe fallback if requestId is missing
  const safeStep = useMemo(() => {
    if ((flow.uiStep === "VERIFY_NEW" || flow.uiStep === "SECOND_FACTOR") && !flow.requestId) {
      return "REQUEST" as const;
    }
    return flow.uiStep;
  }, [flow.uiStep, flow.requestId]);

  const ranDoneRef = useRef(false);

  useEffect(() => {
    if (safeStep !== "DONE") {
      ranDoneRef.current = false;
      return;
    }

    if (ranDoneRef.current) return;
    ranDoneRef.current = true;

    const t = window.setTimeout(() => {
      void flow.finishAfterDone();
    }, 3000);

    return () => window.clearTimeout(t);
  }, [safeStep, flow.finishAfterDone]);


  return (
    <div style={{ maxWidth: 420, margin: "0 auto", padding: 16 }}>
      {safeStep === "REQUEST" && (
        <ChangeEmailStepRequest
          form={step1Form}
          onSubmit={onSubmitRequest}
          loading={loading}
          error={uiError}
          // currentEmail={...} // optional if you want to display it
        />
      )}

      {safeStep === "VERIFY_NEW" && (
        <ChangeEmailStepVerifyNew
          form={step2Form}
          onSubmit={onSubmitVerifyNew}
          loading={loading}
          error={uiError}
          maskedNewEmail={flow.maskedNewEmail}
          // onResend: usually not recommended here because password is required to resend in your flow
        />
      )}

      {safeStep === "SECOND_FACTOR" && (
        <ChangeEmailStepSecondFactor
          form={step3Form}
          onSubmit={onSubmitSecondFactor}
          loading={loading}
          error={uiError}
          secondFactorType={flow.secondFactorType}
        />
      )}

      {safeStep === "DONE" && (
        <div>
          <ChangeEmailStepDone
            // With server-status based flow, you typically only have masked email; showing success only is fine
            newEmail={null}
            onClose={flow.finishAfterDone}
          />
        </div>
      )}

      {/* optional debug/status panel */}
      <div style={{ marginTop: 16, fontSize: 12, color: "#666" }}>
        <div>requestId: {flow.requestId ?? "-"}</div>
        <div>status: {flow.status ?? "-"}</div>
        <div>expiresAt: {flow.expiresAt ?? "-"}</div>

        <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
          <button
            type="button"
            onClick={() => flow.refresh(flow.requestId ?? undefined)}
            disabled={loading}
          >
            Refresh status
          </button>

          <button type="button" onClick={onReset} disabled={loading}>
            Reset
          </button>
        </div>
      </div>
    </div>
  );
}