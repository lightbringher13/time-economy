import { useCallback, useEffect, useMemo, useState } from "react";
import type {
  SignupStatusResponseDto,
  SendSignupOtpResponseDto,
  SignupVerificationTarget,
  VerifySignupOtpResponseDto,
} from "../api/signupApi.types";
import type { ApiErrorResponse } from "@/shared/api/apiError";

function parseIsoToMs(iso: string | null | undefined): number | null {
  if (!iso) return null;
  const ms = Date.parse(iso);
  return Number.isFinite(ms) ? ms : null;
}

function secondsUntil(nowMs: number, targetMs: number | null): number {
  if (!targetMs) return 0;
  const diff = Math.ceil((targetMs - nowMs) / 1000);
  return Math.max(0, diff);
}

function useNowTickMs(enabled: boolean, intervalMs = 1000) {
  const [nowMs, setNowMs] = useState(() => Date.now());

  useEffect(() => {
    if (!enabled) return;
    const id = window.setInterval(() => setNowMs(Date.now()), intervalMs);
    return () => window.clearInterval(id);
  }, [enabled, intervalMs]);

  return nowMs;
}

function fmt(sec: number) {
  const s = Math.max(0, sec);
  const mm = String(Math.floor(s / 60)).padStart(2, "0");
  const ss = String(s % 60).padStart(2, "0");
  return `${mm}:${ss}`;
}

export type SignupOtpUi = ReturnType<typeof useSignupOtpUi>;

type VerifyFeedback = {
  code: string | null;
  message: string | null;
};

const EMPTY_VERIFY_FEEDBACK: VerifyFeedback = { code: null, message: null };

export function useSignupOtpUi(status: SignupStatusResponseDto | null) {
  // ---------------------------
  // SEND cooldown (seeded from SendOtp response)
  // ---------------------------
  const [emailNextSendAtMs, setEmailNextSendAtMs] = useState<number | null>(null);
  const [phoneNextSendAtMs, setPhoneNextSendAtMs] = useState<number | null>(null);

  // ---------------------------
  // VERIFY throttle + attempts (seeded from Verify response/error)
  // ---------------------------
  const [emailVerifyNextAtMs, setEmailVerifyNextAtMs] = useState<number | null>(null);
  const [phoneVerifyNextAtMs, setPhoneVerifyNextAtMs] = useState<number | null>(null);

  const [emailRemainingAttempts, setEmailRemainingAttempts] = useState<number | null>(null);
  const [phoneRemainingAttempts, setPhoneRemainingAttempts] = useState<number | null>(null);

  // last verify UX feedback (so ProgressCard can show LOCKED/EXPIRED/NO_PENDING_OTP etc)
  const [emailVerifyFeedback, setEmailVerifyFeedback] = useState<VerifyFeedback>({ code: null, message: null });
  const [phoneVerifyFeedback, setPhoneVerifyFeedback] = useState<VerifyFeedback>({ code: null, message: null });

  // OTP expiry from STATUS (server truth)
  const emailExpMs = parseIsoToMs(status?.emailOtpExpiresAt);
  const phoneExpMs = parseIsoToMs(status?.phoneOtpExpiresAt);
  const emailOtpPending = Boolean(status?.emailOtpPending);
  const phoneOtpPending = Boolean(status?.phoneOtpPending);

  // Verify-only state is relevant only while the corresponding server OTP is pending.
  // A subsequent send clears the stored values before the next OTP becomes active.
  const activeEmailVerifyNextAtMs = emailOtpPending ? emailVerifyNextAtMs : null;
  const activePhoneVerifyNextAtMs = phoneOtpPending ? phoneVerifyNextAtMs : null;
  const activeEmailRemainingAttempts = emailOtpPending ? emailRemainingAttempts : null;
  const activePhoneRemainingAttempts = phoneOtpPending ? phoneRemainingAttempts : null;
  const activeEmailVerifyFeedback = emailOtpPending
    ? emailVerifyFeedback
    : EMPTY_VERIFY_FEEDBACK;
  const activePhoneVerifyFeedback = phoneOtpPending
    ? phoneVerifyFeedback
    : EMPTY_VERIFY_FEEDBACK;

  // ✅ ticking: no Date.now() inside conditions (stable)
  const ticking =
    Boolean(emailNextSendAtMs) ||
    Boolean(phoneNextSendAtMs) ||
    Boolean(activeEmailVerifyNextAtMs) ||
    Boolean(activePhoneVerifyNextAtMs) ||
    Boolean(emailOtpPending && emailExpMs) ||
    Boolean(phoneOtpPending && phoneExpMs);

  const nowMs = useNowTickMs(ticking, 1000);

  // expiry countdown
  const emailExpiresInSec = emailOtpPending ? secondsUntil(nowMs, emailExpMs) : 0;
  const phoneExpiresInSec = phoneOtpPending ? secondsUntil(nowMs, phoneExpMs) : 0;

  // send cooldown countdown
  const emailResendInSec = secondsUntil(nowMs, emailNextSendAtMs);
  const phoneResendInSec = secondsUntil(nowMs, phoneNextSendAtMs);

  // verify cooldown countdown (throttle)
  const emailVerifyInSec = secondsUntil(nowMs, activeEmailVerifyNextAtMs);
  const phoneVerifyInSec = secondsUntil(nowMs, activePhoneVerifyNextAtMs);

  // ---------------------------
  // APPLY: send response
  // ---------------------------
  const applySendResult = useCallback(
    (target: SignupVerificationTarget, res: SendSignupOtpResponseDto) => {
      const sec = res.retryAfterSeconds;

      // Even on SENT you may now send retryAfterSeconds (cooldown end).
      if (typeof sec === "number") {
        const nextMs = Date.now() + Math.max(0, sec) * 1000;
        if (target === "EMAIL") setEmailNextSendAtMs(nextMs);
        else setPhoneNextSendAtMs(nextMs);
      }

      // New send means old verify feedback is stale
      if (target === "EMAIL") {
        setEmailVerifyFeedback({ code: null, message: null });
        setEmailVerifyNextAtMs(null);
        setEmailRemainingAttempts(null);
      } else {
        setPhoneVerifyFeedback({ code: null, message: null });
        setPhoneVerifyNextAtMs(null);
        setPhoneRemainingAttempts(null);
      }
    },
    []
  );

  // ---------------------------
  // APPLY: verify success response (2xx)
  // ---------------------------
  const applyVerifyResult = useCallback(
    (target: SignupVerificationTarget, res: VerifySignupOtpResponseDto) => {
      // For VERIFIED, status will soon update and effect will clear anyway.
      // Still: keep feedback clean now.
      if (target === "EMAIL") {
        setEmailVerifyFeedback({ code: res.outcome, message: null });
      } else {
        setPhoneVerifyFeedback({ code: res.outcome, message: null });
      }

      // only set attempts if server provided it
      if (typeof res.remainingAttempts === "number") {
        if (target === "EMAIL") setEmailRemainingAttempts(res.remainingAttempts);
        else setPhoneRemainingAttempts(res.remainingAttempts);
      }

      // only set retry if provided
      if (typeof res.retryAfterSeconds === "number") {
        const next = Date.now() + Math.max(0, res.retryAfterSeconds) * 1000;
        if (target === "EMAIL") setEmailVerifyNextAtMs(next);
        else setPhoneVerifyNextAtMs(next);
      }
    },
    []
  );

  // ---------------------------
  // APPLY: verify error (4xx) — uses ApiErrorResponse.details
  // ---------------------------
  const applyVerifyError = useCallback(
    (target: SignupVerificationTarget, apiErr: ApiErrorResponse | null) => {
      if (!apiErr) return;

      // Always store last verify error code/message so ProgressCard can display it
      const fb: VerifyFeedback = { code: apiErr.code ?? null, message: apiErr.message ?? null };
      if (target === "EMAIL") setEmailVerifyFeedback(fb);
      else setPhoneVerifyFeedback(fb);

      const d = apiErr.details;

      // ✅ attempts: ONLY overwrite if provided as number (keeps old attempts on THROTTLED that returns null)
      if (typeof d?.remainingAttempts === "number") {
        if (target === "EMAIL") setEmailRemainingAttempts(d.remainingAttempts);
        else setPhoneRemainingAttempts(d.remainingAttempts);
      }

      // retry-after => verify throttle time
      if (typeof d?.retryAfterSeconds === "number") {
        const next = Date.now() + Math.max(0, d.retryAfterSeconds) * 1000;
        if (target === "EMAIL") setEmailVerifyNextAtMs(next);
        else setPhoneVerifyNextAtMs(next);
      }
    },
    []
  );

  // optional: call when user edits destination (new identity => clear send cooldown)
  const resetCooldown = useCallback((target: SignupVerificationTarget) => {
    if (target === "EMAIL") setEmailNextSendAtMs(null);
    else setPhoneNextSendAtMs(null);
  }, []);

  // optional: clear verify feedback manually (e.g., on page enter)
  const clearVerifyFeedback = useCallback((target: SignupVerificationTarget) => {
    if (target === "EMAIL") setEmailVerifyFeedback({ code: null, message: null });
    else setPhoneVerifyFeedback({ code: null, message: null });
  }, []);

  return useMemo(
    () => ({
      email: {
        // OTP expiry
        expiresInSec: emailExpiresInSec,
        expiresText: emailOtpPending ? fmt(emailExpiresInSec) : null,
        isExpired: Boolean(emailOtpPending && emailExpMs && nowMs >= emailExpMs),

        // SEND cooldown
        resendInSec: emailResendInSec,
        resendText: emailResendInSec > 0 ? fmt(emailResendInSec) : null,
        canSend: emailResendInSec === 0,

        // VERIFY cooldown + attempts + last feedback
        verifyInSec: emailVerifyInSec,
        verifyText: emailVerifyInSec > 0 ? fmt(emailVerifyInSec) : null,
        canVerify: emailVerifyInSec === 0,
        remainingAttempts: activeEmailRemainingAttempts,

        lastVerifyCode: activeEmailVerifyFeedback.code,
        lastVerifyMessage: activeEmailVerifyFeedback.message,
      },

      phone: {
        expiresInSec: phoneExpiresInSec,
        expiresText: phoneOtpPending ? fmt(phoneExpiresInSec) : null,
        isExpired: Boolean(phoneOtpPending && phoneExpMs && nowMs >= phoneExpMs),

        resendInSec: phoneResendInSec,
        resendText: phoneResendInSec > 0 ? fmt(phoneResendInSec) : null,
        canSend: phoneResendInSec === 0,

        verifyInSec: phoneVerifyInSec,
        verifyText: phoneVerifyInSec > 0 ? fmt(phoneVerifyInSec) : null,
        canVerify: phoneVerifyInSec === 0,
        remainingAttempts: activePhoneRemainingAttempts,

        lastVerifyCode: activePhoneVerifyFeedback.code,
        lastVerifyMessage: activePhoneVerifyFeedback.message,
      },

      applySendResult,
      applyVerifyResult,
      applyVerifyError,
      resetCooldown,
      clearVerifyFeedback,
    }),
    [
      nowMs,
      emailOtpPending,
      phoneOtpPending,
      emailExpMs,
      phoneExpMs,
      emailExpiresInSec,
      phoneExpiresInSec,
      emailResendInSec,
      phoneResendInSec,
      emailVerifyInSec,
      phoneVerifyInSec,
      activeEmailRemainingAttempts,
      activePhoneRemainingAttempts,
      activeEmailVerifyFeedback,
      activePhoneVerifyFeedback,
      applySendResult,
      applyVerifyResult,
      applyVerifyError,
      resetCooldown,
      clearVerifyFeedback,
    ]
  );
}
