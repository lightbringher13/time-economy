export type SendEmailCodeRequest = {
  email: string;
};

export type SendEmailCodeResponse = {
  code: string;
};

export type VerifyEmailCodeRequest = {
  email: string;
  code: string;
};

export type VerifyEmailCodeResponse = {
  verified: boolean;
};

export type EmailVerificationStatusResponse = {
  verified: boolean;
};