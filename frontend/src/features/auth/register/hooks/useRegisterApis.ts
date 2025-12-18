import { useEffect, useRef, useState } from "react";
import type { UseFormReturn } from "react-hook-form";
import type { RegisterFormValues } from "../schemas/registerForm";
import { signupBootstrapApi, updateSignupProfileApi } from "../api/signupSessionApi";
import { registerApi } from "../api/registerApi";
import { useNavigate } from "react-router-dom";
import { ROUTES } from "@/routes/paths";
import { isApiError } from "@/shared/api/apiClient";

import { sendSignupOtpApi,verifySignupOtpApi } from "../api/signupSessionApi";

export function useRegisterApis(form: UseFormReturn<RegisterFormValues>) {
  const navigate = useNavigate();
  const { reset, watch, getValues, setValue } = form;

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
      if (!email?.trim()) return;

      if (autosaveTimerRef.current) window.clearTimeout(autosaveTimerRef.current);

      autosaveTimerRef.current = window.setTimeout(() => {
        updateSignupProfileApi({
          email: email.trim(),
          phoneNumber: phoneNumber?.trim() || null,
          name: name?.trim() || null,
          gender: gender || null,
          birthDate: birthDate || null,
        }).catch((err) => console.error("[Register] autosave failed", err));
      }, 600);
    });

    return () => {
      subscription.unsubscribe();
      if (autosaveTimerRef.current) window.clearTimeout(autosaveTimerRef.current);
    };
  }, [bootstrapLoading, watch]);

  // ========= 3) EMAIL (verification-challenge) =========
  const handleSendEmailCode = async () => {
    setError(null);
    setVerificationInfo(null);

    const email = getValues("email")?.trim();
    if (!email) {
      setError("Email is required before sending code.");
      return;
    }

    setSendEmailCodeLoading(true);
    try {
      const res = await sendSignupOtpApi({
        target: "EMAIL",
      });

      // dev-only
      setVerificationInfo(
        `Code sent. TTL=${res.ttlMinutes}min, to=${res.maskedDestination} (dev: check backend logs / email)`
      );
    } catch (err) {
      console.error("[Register] send email code failed", err);

      if (isApiError(err)) {
        const api = err.response?.data;
        setError(api?.message || "Failed to send verification code.");
      } else {
        setError("Unexpected error occurred.");
      }
    } finally {
      setSendEmailCodeLoading(false);
    }
  };

  const handleVerifyEmailCode = async () => {
    setError(null);
    setVerificationInfo(null);

    const email = getValues("email")?.trim();
    const code = getValues("emailCode")?.trim();

    if (!email) {
      setError("Email is required.");
      return;
    }
    if (!code) {
      setError("Verification code is required.");
      return;
    }

    setVerifyEmailCodeLoading(true);
    try {
      const res = await verifySignupOtpApi({
        target: "EMAIL",
        code,
      });

      if (res.success) {
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

  // ========= 4) PHONE (verification-challenge) =========
  const handleSendPhoneCode = async () => {
    setError(null);
    setPhoneVerificationInfo(null);

    const phone = getValues("phoneNumber")?.trim();
    if (!phone) {
      setError("Phone number is required before sending verification code.");
      return;
    }

    setSendPhoneCodeLoading(true);
    try {
      const res = await sendSignupOtpApi({
        target: "PHONE",
      });

      setPhoneVerificationInfo(
        `SMS sent. TTL=${res.ttlMinutes}min (dev: check backend logs).`
      );
    } catch (err) {
      console.error("[Register] send phone code failed", err);

      if (isApiError(err)) {
        const api = err.response?.data;
        setError(api?.message ?? "Failed to send phone verification code.");
      } else {
        setError("Failed to send phone verification code.");
      }
    } finally {
      setSendPhoneCodeLoading(false);
    }
  };

  const handleVerifyPhoneCode = async () => {
    setError(null);
    setPhoneVerificationInfo(null);

    const phone = getValues("phoneNumber")?.trim();
    const code = getValues("phoneCode")?.trim();

    if (!phone) {
      setError("Phone number is required.");
      return;
    }
    if (!code) {
      setError("Phone verification code is required.");
      return;
    }

    setVerifyPhoneCodeLoading(true);
    try {
      const res = await verifySignupOtpApi({
        target: "PHONE",
        code,
      });

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

      if (isApiError(err)) {
        const code = err.response?.data?.code;
        const message = err.response?.data?.message;

        // You can keep these if backend still returns them,
        // otherwise handle generic errors.
        switch (code) {
          case "EMAIL_ALREADY_USED":
            setError(message ?? "This email is already in use.");
            setEmailVerified(false);
            setVerificationInfo(null);
            setValue("emailCode", "");
            break;

          case "PHONE_ALREADY_USED":
            setError(message ?? "This phone number is already in use.");
            setPhoneVerified(false);
            setPhoneVerificationInfo(null);
            setValue("phoneCode", "");
            break;

          default:
            setError(message ?? "Failed to register. Please check your information.");
        }
      } else {
        setError("Failed to register. Please check your information.");
      }
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

    submitProps: { loading },

    onSubmit,
  };
}