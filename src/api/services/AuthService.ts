import { ACCESS_TOKEN_KEY, profileClient } from "@/api/client";
import type { AuthSession, User } from "@/domain";

interface LoginResponse {
  accessToken: string;
  userId: string;
  user: UserResponse;
}

interface UserResponse {
  id: string;
  email: string;
  username: string;
  firstName?: string | null;
  lastName?: string | null;
  avatarUrl?: string | null;
  bio?: string | null;
  emailVerified?: boolean;
  role?: string;
  status?: string;
}

export interface RegistrationStartedResponse {
  email: string;
  expiresInSeconds: number;
  message: string;
}

interface RegisterPayload {
  email: string;
  username: string;
  password: string;
}

interface LoginPayload {
  identifier: string;
  password: string;
}

interface UpdateProfilePayload {
  email?: string;
  firstName?: string;
  lastName?: string;
  bio?: string;
}

const ACTIVE_USER_ID_KEY = "sparrow.profile.active_user_id";
const USERS_KEY = "sparrow.profile.users";
const TOKENS_KEY = "sparrow.profile.tokens";

function setSharedCookie(name: string, value: string) {
  const d = new Date();
  d.setTime(d.getTime() + 365 * 24 * 60 * 60 * 1000);
  document.cookie = `${name}=${value};expires=${d.toUTCString()};path=/`;
}

function getSharedCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
  if (match) return match[2];
  return null;
}

function deleteSharedCookie(name: string) {
  document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/`;
}

function normalizeUser(user: UserResponse): User {
  return {
    id: user.id,
    username: user.username,
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    avatarUrl: user.avatarUrl,
    bio: user.bio,
    emailVerified: user.emailVerified,
    role: user.role,
  };
}

export interface UserPublicDto {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
}

export class AuthService {
  static async searchUsers(query: string): Promise<UserPublicDto[]> {
    const response = await profileClient.get<UserPublicDto[]>("/search/search", { params: { q: query } });
    return response.data;
  }

  static getActiveUserId(): string | null {
    return getSharedCookie(ACTIVE_USER_ID_KEY);
  }

  static getStoredAccounts(): User[] {
    const usersStr = window.localStorage.getItem(USERS_KEY);
    if (!usersStr) return [];
    try {
      const usersMap = JSON.parse(usersStr) as Record<string, User>;
      return Object.values(usersMap);
    } catch {
      return [];
    }
  }

  static getStoredSession(): User | null {
    const activeId = this.getActiveUserId();
    if (!activeId) return null;
    const usersStr = window.localStorage.getItem(USERS_KEY);
    if (!usersStr) return null;
    try {
      const usersMap = JSON.parse(usersStr) as Record<string, User>;
      return usersMap[activeId] || null;
    } catch {
      return null;
    }
  }

  static getAccessToken(): string | null {
    const activeId = this.getActiveUserId();
    if (!activeId) return null;
    const tokensStr = window.localStorage.getItem(TOKENS_KEY);
    if (!tokensStr) return null;
    try {
      const tokensMap = JSON.parse(tokensStr) as Record<string, string>;
      return tokensMap[activeId] || null;
    } catch {
      return null;
    }
  }

  static switchAccount(userId: string) {
    setSharedCookie(ACTIVE_USER_ID_KEY, userId);
    const token = this.getAccessToken();
    if (token) {
      window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
    } else {
      window.localStorage.removeItem(ACCESS_TOKEN_KEY);
    }
  }

  static async login(payload: LoginPayload): Promise<User> {
    const response = await profileClient.post<LoginResponse>("/auth/login", {
      ...payload,
      deviceId: window.navigator.userAgent,
    });
    const user = normalizeUser(response.data.user);
    this.persist(response.data.accessToken, user);
    return user;
  }

  static async register(payload: RegisterPayload): Promise<RegistrationStartedResponse> {
    const response = await profileClient.post<RegistrationStartedResponse>("/auth/register", payload);
    return response.data;
  }

  static async confirmRegistration(email: string, code: string): Promise<User> {
    const response = await profileClient.post<LoginResponse>("/auth/confirm-registration", {
      email,
      code,
      deviceId: window.navigator.userAgent,
    });
    const user = normalizeUser(response.data.user);
    this.persist(response.data.accessToken, user);
    return user;
  }

  static async resendRegistrationCode(email: string): Promise<RegistrationStartedResponse> {
    const response = await profileClient.post<RegistrationStartedResponse>("/auth/resend-registration-code", { email });
    return response.data;
  }

  static async refresh(): Promise<User | null> {
    const activeId = this.getActiveUserId();
    try {
      const response = await profileClient.post<{ accessToken: string }>(
        "/auth/refresh",
        {},
        { headers: activeId ? { "X-User-Id": activeId } : {} },
      );
      this.updateActiveToken(response.data.accessToken);
      const user = await this.getMe();
      this.persist(response.data.accessToken, user);
      return user;
    } catch {
      if (activeId) this.removeAccount(activeId);
      return null;
    }
  }

  static async getMe(): Promise<User> {
    const response = await profileClient.get<User>("/users/me");
    const user = response.data;
    this.persistUserOnly(user);
    return user;
  }

  static async updateProfile(payload: UpdateProfilePayload): Promise<User> {
    const response = await profileClient.patch<User>("/users/me", payload);
    this.persistUserOnly(response.data);
    return response.data;
  }

  static async uploadAvatar(file: File): Promise<User> {
    const form = new FormData();
    form.append("file", file);
    const response = await profileClient.post<User>("/users/me/avatar", form);
    this.persistUserOnly(response.data);
    return response.data;
  }

  static async verifyEmail(code: string): Promise<void> {
    await profileClient.post("/auth/verify-email", { code });
  }

  static async resendVerification(): Promise<void> {
    await profileClient.post("/auth/resend-verification");
  }

  static async forgotPassword(identifier: string): Promise<void> {
    await profileClient.post("/auth/forgot-password", { identifier });
  }

  static async resetPassword(identifier: string, code: string, newPassword: string): Promise<void> {
    await profileClient.post("/auth/reset-password", { identifier, code, newPassword });
  }

  static async logout(): Promise<void> {
    const activeId = this.getActiveUserId();
    try {
      await profileClient.post(
        "/auth/logout",
        {},
        {
          headers: activeId ? { "X-User-Id": activeId } : {},
        },
      );
    } finally {
      if (activeId) this.removeAccount(activeId);
    }
  }

  static async logoutAll(): Promise<void> {
    const activeId = this.getActiveUserId();
    try {
      await profileClient.post("/auth/logout-all");
    } finally {
      if (activeId) this.removeAccount(activeId);
    }
  }

  static async getSessions(): Promise<AuthSession[]> {
    const response = await profileClient.get<AuthSession[]>("/sessions");
    return response.data;
  }

  static async revokeSession(id: string): Promise<void> {
    await profileClient.delete(`/sessions/${id}`);
  }

  private static updateActiveToken(accessToken: string) {
    const activeId = this.getActiveUserId();
    if (!activeId) return;
    try {
      const tokensMap = JSON.parse(window.localStorage.getItem(TOKENS_KEY) || "{}") as Record<string, string>;
      tokensMap[activeId] = accessToken;
      window.localStorage.setItem(TOKENS_KEY, JSON.stringify(tokensMap));
      window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    } catch {}
  }

  private static persistUserOnly(user: User) {
    try {
      const usersMap = JSON.parse(window.localStorage.getItem(USERS_KEY) || "{}") as Record<string, User>;
      usersMap[user.id] = user;
      window.localStorage.setItem(USERS_KEY, JSON.stringify(usersMap));
    } catch {}
  }

  private static persist(accessToken: string, user: User) {
    setSharedCookie(ACTIVE_USER_ID_KEY, user.id);
    window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);

    let usersMap: Record<string, User> = {};
    try {
      usersMap = JSON.parse(window.localStorage.getItem(USERS_KEY) || "{}");
    } catch {}
    usersMap[user.id] = user;
    window.localStorage.setItem(USERS_KEY, JSON.stringify(usersMap));

    let tokensMap: Record<string, string> = {};
    try {
      tokensMap = JSON.parse(window.localStorage.getItem(TOKENS_KEY) || "{}");
    } catch {}
    tokensMap[user.id] = accessToken;
    window.localStorage.setItem(TOKENS_KEY, JSON.stringify(tokensMap));
  }

  private static removeAccount(userId: string) {
    let usersMap: Record<string, User> = {};
    try {
      usersMap = JSON.parse(window.localStorage.getItem(USERS_KEY) || "{}");
    } catch {}
    delete usersMap[userId];
    window.localStorage.setItem(USERS_KEY, JSON.stringify(usersMap));

    let tokensMap: Record<string, string> = {};
    try {
      tokensMap = JSON.parse(window.localStorage.getItem(TOKENS_KEY) || "{}");
    } catch {}
    delete tokensMap[userId];
    window.localStorage.setItem(TOKENS_KEY, JSON.stringify(tokensMap));

    const activeId = this.getActiveUserId();
    if (activeId === userId) {
      const remainingUsers = Object.keys(usersMap);
      if (remainingUsers.length > 0) {
        this.switchAccount(remainingUsers[0]);
      } else {
        deleteSharedCookie(ACTIVE_USER_ID_KEY);
        window.localStorage.removeItem(ACCESS_TOKEN_KEY);
      }
    }
  }
}
