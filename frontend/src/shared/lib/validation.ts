export const isEmail = (value: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
export const isUsername = (value: string) => value.trim().length >= 3;
export const isPassword = (value: string) => value.length >= 8;
export const isVerificationCode = (value: string) => /^\d{6}$/.test(value);
