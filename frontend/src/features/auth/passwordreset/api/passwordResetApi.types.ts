// "비밀번호 재설정 메일 보내기" 폼
export type PasswordResetRequest = {
  email: string;
};

// "비밀번호 재설정" 폼 (토큰은 URL에서 가져옴)
export type PasswordResetConfirm = {
  newPassword: string;
  confirmPassword: string;
};