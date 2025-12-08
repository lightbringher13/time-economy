// features/auth/hooks/useRegisterApis.ts
import { useEffect, useRef, useState } from "react";
import type { UseFormReturn } from "react-hook-form";
import type { RegisterFormValues } from "../types/registerForm";

import {
  signupBootstrapApi,
  updateSignupProfileApi,
  sendEmailCodeApi,
  verifyEmailCodeApi,
  requestPhoneCodeApi,
  verifyPhoneCodeApi,
  registerApi,
} from "../api/authApi";
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";

export function useRegisterApis(form: UseFormReturn<RegisterFormValues>) {
  const navigate = useNavigate();
  const { reset, watch, getValues } = form;

  const [bootstrapLoading, setBootstrapLoading] = useState(true);
  const [loading, setLoading] = useState(false);

  const [sendEmailCodeLoading, setSendEmailCodeLoading] = useState(false);
  const [verifyEmailCodeLoading, setVerifyEmailCodeLoading] = useState(false);
  const [sendPhoneCodeLoading, setSendPhoneCodeLoading] = useState(false);
  const [verifyPhoneCodeLoading, setVerifyPhoneCodeLoading] = useState(false);

  const [emailVerified, setEmailVerified] = useState(false);
  const [phoneVerified, setPhoneVerified] = useState(false);

  const [verificationInfo, setVerificationInfo] = useState<string | null>(null);
  const [phoneVerificationInfo, setPhoneVerificationInfo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const autosaveTimerRef = useRef<number | null>(null);

  // ========= 1) BOOTSTRAP =========
  useEffect(() => {
    const bootstrap = async () => {
      try {
        setBootstrapLoading(true);
        const data = await signupBootstrapApi();

        if (data.hasSession) {
          reset({
            email: data.email ?? "",
            emailCode: "",
            password: "",
            passwordConfirm: "",
            phoneNumber: data.phoneNumber ?? "",
            phoneCode: "",
            name: data.name ?? "",
            gender: (data.gender ?? "") as RegisterFormValues["gender"],
            birthDate: data.birthDate ?? "",
          });

          setEmailVerified(!!data.emailVerified);
          setPhoneVerified(!!data.phoneVerified);
        }
      } catch (e) {
        console.error("[Register] bootstrap failed", e);
      } finally {
        setBootstrapLoading(false);
      }
    };

    bootstrap();
  }, [reset]);

  // ========= 2) AUTOSAVE PROFILE =========
  useEffect(() => {
  if (bootstrapLoading) return;

  const subscription = watch((values) => {
    const { email, name, phoneNumber, gender, birthDate } = values;

    // 세션은 "이메일이 한 번이라도 채워진 상태"에서만 저장
    if (!email?.trim()) return;

    if (autosaveTimerRef.current) {
      window.clearTimeout(autosaveTimerRef.current);
    }

    autosaveTimerRef.current = window.setTimeout(() => {
      updateSignupProfileApi({
        email: email.trim(),                        // ✅ 추가
        phoneNumber: phoneNumber?.trim() || null,   // ✅ 같이 저장
        name: name?.trim() || null,
        gender: gender || null,
        birthDate: birthDate || null,
      }).catch((err) => {
        console.error("[Register] autosave failed", err);
      });
    }, 600);
  });

  return () => {
    subscription.unsubscribe();
    if (autosaveTimerRef.current) {
      window.clearTimeout(autosaveTimerRef.current);
    }
  };
}, [bootstrapLoading, watch]);

  // ========= 3) EMAIL APIs =========
  const handleSendEmailCode = async () => {
    setError(null);
    setVerificationInfo(null);

    const email = getValues("email");
    if (!email?.trim()) {
      setError("Email is required before sending code.");
      return;
    }

    setSendEmailCodeLoading(true);
    try {
      const code = await sendEmailCodeApi({ email });
      setVerificationInfo(`Verification code (dev): ${code}`);
    } catch (err) {
      console.error("[Register] send email code failed", err);
      setError("Failed to send verification code. Please try again.");
    } finally {
      setSendEmailCodeLoading(false);
    }
  };

  const handleVerifyEmailCode = async () => {
    setError(null);
    setVerificationInfo(null);

    const email = getValues("email");
    const emailCode = getValues("emailCode");

    if (!email?.trim()) {
      setError("Email is required.");
      return;
    }
    if (!emailCode?.trim()) {
      setError("Verification code is required.");
      return;
    }

    setVerifyEmailCodeLoading(true);
    try {
      const res = await verifyEmailCodeApi({ email, code: emailCode });
      if (res.verified) {
        setEmailVerified(true);
        setVerificationInfo("Email verified successfully.");
      } else {
        setEmailVerified(false);
        setError("Invalid or expired verification code.");
      }
    } catch (err) {
      console.error("[Register] verify email code failed", err);
      setError("Failed to verify code. Please try again.");
      setEmailVerified(false);
    } finally {
      setVerifyEmailCodeLoading(false);
    }
  };

  // ========= 4) PHONE APIs =========
  const handleSendPhoneCode = async () => {
    setError(null);
    setPhoneVerificationInfo(null);

    const phoneNumber = getValues("phoneNumber");
    if (!phoneNumber?.trim()) {
      setError("Phone number is required before sending code.");
      return;
    }

    setSendPhoneCodeLoading(true);
    try {
      await requestPhoneCodeApi({ phoneNumber });
      setPhoneVerificationInfo("Phone verification code sent. (dev: check backend logs)");
    } catch (err) {
      console.error("[Register] send phone code failed", err);
      setError("Failed to send phone verification code. Please try again.");
    } finally {
      setSendPhoneCodeLoading(false);
    }
  };

  const handleVerifyPhoneCode = async () => {
    setError(null);
    setPhoneVerificationInfo(null);

    const phoneNumber = getValues("phoneNumber");
    const phoneCode = getValues("phoneCode");

    if (!phoneNumber?.trim()) {
      setError("Phone number is required.");
      return;
    }
    if (!phoneCode?.trim()) {
      setError("Phone verification code is required.");
      return;
    }

    setVerifyPhoneCodeLoading(true);
    try {
      const res = await verifyPhoneCodeApi({ phoneNumber, code: phoneCode });
      if (res.success) {
        setPhoneVerified(true);
        setPhoneVerificationInfo("Phone verified successfully.");
      } else {
        setPhoneVerified(false);
        setError("Invalid or expired phone verification code.");
      }
    } catch (err) {
      console.error("[Register] verify phone code failed", err);
      setError("Failed to verify phone code. Please try again.");
      setPhoneVerified(false);
    } finally {
      setVerifyPhoneCodeLoading(false);
    }
  };

  // ========= 5) SUBMIT =========
  const onSubmit = async (values: RegisterFormValues) => {
    setError(null);

    if (!emailVerified) {
      setError("Please verify your email before registering.");
      return;
    }
    if (!phoneVerified) {
      setError("Please verify your phone number before registering.");
      return;
    }

    if (values.password !== values.passwordConfirm) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      await registerApi({
        email: values.email,
        password: values.password,
        phoneNumber: values.phoneNumber,
        name: values.name,
        gender: values.gender,
        birthDate: values.birthDate,
      });

      alert("Registration successful! Please log in.");
      navigate(ROUTES.LOGIN, { replace: true });
    } catch (err) {
      console.error("[Register] register failed", err);
      setError("Failed to register. Please check your information.");
    } finally {
      setLoading(false);
    }
  };

  return {
    bootstrapLoading,
    error,

    emailSectionProps: {
      emailVerified,
      sendEmailCodeLoading,
      verifyEmailCodeLoading,
      verificationInfo,
      onSendCode: handleSendEmailCode,
      onVerifyCode: handleVerifyEmailCode,
    },

    phoneSectionProps: {
      phoneVerified,
      sendPhoneCodeLoading,
      verifyPhoneCodeLoading,
      phoneVerificationInfo,
      onSendPhoneCode: handleSendPhoneCode,
      onVerifyPhoneCode: handleVerifyPhoneCode,
    },

    submitProps: {
      loading,
    },

    onSubmit,
  };
}