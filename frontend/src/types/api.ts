export type ApiErrorResponse = {
  success?: boolean;  // gateway 있으면 포함됨
  service?: string;   // gateway 서비스명
  code: string;       // "EMAIL_ALREADY_USED", "ACCESS_TOKEN_EXPIRED"
  message: string;
  status?: number;
  timestamp?: string;
  path?: string;
};