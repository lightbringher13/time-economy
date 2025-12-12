export type RegisterRequest = {
  email: string;
  password: string;
  phoneNumber: string;
  name: string;
  gender: string;        // "MALE" | "FEMALE" | "OTHER" | etc. (depending on UI)
  birthDate: string;     // "yyyy-MM-dd"
};

export type RegisterResponse = {
  userId: number;
  email: string;
};