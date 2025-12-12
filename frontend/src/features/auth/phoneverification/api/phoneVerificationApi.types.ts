export type RequestPhoneVerificationCodeRequest = {
  phoneNumber: string;
  countryCode?: string; // optional, backend defaults to +82
};

export type VerifyPhoneCodeRequest = {
  phoneNumber: string;
  code: string;
};

export type VerifyPhoneCodeResponse = {
  success: boolean;
};
