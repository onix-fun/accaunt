import { describe, expect, it } from "vitest";
import { isEmail, isPassword, isUsername, isVerificationCode } from "./validation";

describe("form validation", () => {
  it("validates identifiers, passwords, and codes before submit", () => {
    expect(isEmail("user@example.com")).toBe(true);
    expect(isEmail("invalid")).toBe(false);
    expect(isUsername("ab")).toBe(false);
    expect(isUsername("docup")).toBe(true);
    expect(isPassword("1234567")).toBe(false);
    expect(isPassword("12345678")).toBe(true);
    expect(isVerificationCode("123456")).toBe(true);
    expect(isVerificationCode("12345a")).toBe(false);
  });
});
