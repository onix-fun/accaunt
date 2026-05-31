export interface User {
  id: string;
  username: string;
  email?: string;
  firstName?: string | null;
  lastName?: string | null;
  avatarUrl?: string | null;
  bio?: string | null;
  emailVerified?: boolean;
  role?: string;
}

export interface AuthSession {
  id: string;
  isCurrent?: boolean;
  deviceId?: string | null;
  userAgent?: string | null;
  ipAddress?: string | null;
  lastUsedAt?: string | null;
  expiresAt?: string | null;
  createdAt?: string | null;
}

export interface Permission {
  userId: string;
  deviceId: string;
  role: 'OWNER' | 'USER' | 'VIEWER';
}

export type Role = Permission['role'];
