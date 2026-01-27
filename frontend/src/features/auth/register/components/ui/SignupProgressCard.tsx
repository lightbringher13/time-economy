// src/features/auth/register/components/SignupProgressCard.tsx
import { useMemo } from "react";
import { useSignupFlow } from "../../hooks/SignupFlowContext";
import { useSignupOtpUiContext } from "../../hooks/SignupOtpUiContext";

type StepStatus = "DONE" | "ACTIVE" | "PENDING" | "BLOCKED";
type StepKey = "email" | "emailVerified" | "phone" | "phoneVerified" | "profile" | "completed";

type Step = {
  key: StepKey;
  label: string;
  status: StepStatus;
  extraLines: string[]; // ✅ vertical lines
};

function Badge({ status }: { status: StepStatus }) {
  const text =
    status === "DONE" ? "✅" :
    status === "ACTIVE" ? "🟦" :
    status === "PENDING" ? "⏳" : "⛔";
  return <span style={{ width: 30, display: "inline-block", fontSize: 18 }}>{text}</span>;
}

function statusText(status: StepStatus) {
  if (status === "DONE") return "Done";
  if (status === "ACTIVE") return "In progress";
  if (status === "PENDING") return "Not started";
  return "Blocked";
}

export function SignupProgressCard() {
  const flow = useSignupFlow();
  const ui = useSignupOtpUiContext();
  const v = flow.view;

  const profileReady = Boolean(v.name) && Boolean(v.gender) && Boolean(v.birthDate);

  const steps: Step[] = useMemo(() => {
    const emailStep: StepStatus =
      v.emailVerified ? "DONE" : v.emailOtpPending ? "ACTIVE" : "PENDING";

    const emailVerified: StepStatus =
      v.emailVerified ? "DONE" : "PENDING";

    const phoneStep: StepStatus =
      !v.emailVerified ? "BLOCKED"
      : v.phoneVerified ? "DONE"
      : v.phoneOtpPending ? "ACTIVE" : "PENDING";

    const phoneVerified: StepStatus =
      !v.emailVerified ? "BLOCKED"
      : v.phoneVerified ? "DONE" : "PENDING";

    const profile: StepStatus =
      !v.phoneVerified ? "BLOCKED"
      : profileReady ? "DONE" : "ACTIVE";

    const completed: StepStatus =
      v.state === "COMPLETED" ? "DONE" : "PENDING";

    // -------- Email extras (VERTICAL) --------
    const emailExtraLines: string[] = [];

    if (v.emailOtpPending) {
      emailExtraLines.push(
        ui.email.isExpired
          ? "OTP expired — resend a new code."
          : `Code sent to ${v.email ?? "(email)"}`
      );

      if (!ui.email.isExpired) {
        emailExtraLines.push(`Expires in ${ui.email.expiresText ?? "--:--"}`);
      }
    } else if (v.email) {
      emailExtraLines.push(`Email: ${v.email}`);
    }

    // resend cooldown
    emailExtraLines.push(ui.email.resendText ? `Resend in ${ui.email.resendText}` : "Resend available");

    // verify throttle (optional; only if you seed it)
    if (ui.email.verifyText) emailExtraLines.push(`Verify available in ${ui.email.verifyText}`);

    // remaining attempts (optional)
    if (typeof ui.email.remainingAttempts === "number") {
      emailExtraLines.push(`Attempts left: ${ui.email.remainingAttempts}`);
    }

    // -------- Phone extras (VERTICAL) --------
    const phoneExtraLines: string[] = [];

    if (v.phoneOtpPending) {
      phoneExtraLines.push(
        ui.phone.isExpired
          ? "OTP expired — resend a new code."
          : `Code sent to ${v.phoneNumber ?? "(phone)"}`
      );

      if (!ui.phone.isExpired) {
        phoneExtraLines.push(`Expires in ${ui.phone.expiresText ?? "--:--"}`);
      }
    } else if (v.phoneNumber) {
      phoneExtraLines.push(`Phone: ${v.phoneNumber}`);
    }

    phoneExtraLines.push(ui.phone.resendText ? `Resend in ${ui.phone.resendText}` : "Resend available");

    if (ui.phone.verifyText) phoneExtraLines.push(`Verify available in ${ui.phone.verifyText}`);

    if (typeof ui.phone.remainingAttempts === "number") {
      phoneExtraLines.push(`Attempts left: ${ui.phone.remainingAttempts}`);
    }

    return [
      { key: "email", label: "Email verification", status: emailStep, extraLines: emailExtraLines },
      { key: "emailVerified", label: "Email confirmed", status: emailVerified, extraLines: [] },
      { key: "phone", label: "Phone verification", status: phoneStep, extraLines: phoneExtraLines },
      { key: "phoneVerified", label: "Phone confirmed", status: phoneVerified, extraLines: [] },
      { key: "profile", label: "Profile", status: profile, extraLines: profileReady ? ["Profile completed"] : [] },
      { key: "completed", label: "Finish", status: completed, extraLines: [] },
    ];
  }, [
    v.email,
    v.emailVerified,
    v.emailOtpPending,
    v.phoneNumber,
    v.phoneVerified,
    v.phoneOtpPending,
    v.state,
    profileReady,

    ui.email.expiresText,
    ui.email.isExpired,
    ui.email.resendText,
    ui.email.verifyText,
    ui.email.remainingAttempts,

    ui.phone.expiresText,
    ui.phone.isExpired,
    ui.phone.resendText,
    ui.phone.verifyText,
    ui.phone.remainingAttempts,
  ]);

  return (
    <div
      style={{
        border: "1px solid #e5e7eb",
        borderRadius: 16,
        padding: 18,
        marginBottom: 16,
        background: "white",
        boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
      }}
    >
      <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 10 }}>
        Signup progress
      </div>

      <div style={{ display: "grid", gap: 10 }}>
        {steps.map((s) => (
          <div key={s.key} style={{ display: "flex", alignItems: "flex-start", gap: 10 }}>
            <Badge status={s.status} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, display: "flex", justifyContent: "space-between", gap: 12 }}>
                <span style={{ fontWeight: 600 }}>{s.label}</span>
                <span style={{ fontSize: 12, opacity: 0.65 }}>{statusText(s.status)}</span>
              </div>

              {s.extraLines.length > 0 ? (
                <div style={{ marginTop: 6, display: "grid", gap: 3 }}>
                  {s.extraLines.map((line, idx) => (
                    <div key={idx} style={{ fontSize: 13, opacity: 0.85 }}>
                      {line}
                    </div>
                  ))}
                </div>
              ) : null}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}