import axios from "axios";

export const ACCESS_TOKEN_KEY = "sparrow.profile.access_token";
export const API_BASE_URL = (import.meta.env.VITE_API_URL || "/api").replace(/\/$/, "");
export const DOMAIN_BASE_URL = `${API_BASE_URL}/domain`;
export const ANALYTICS_BASE_URL = `${API_BASE_URL}/analytics`;
export const PROFILE_BASE_URL = API_BASE_URL;

export function getStoredAccessToken(): string | null {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function contactsWsBaseUrl(): string {
  const configured = import.meta.env.VITE_CONTACTS_WS_URL as string | undefined;
  if (configured) return configured.replace(/\/$/, "");

  const apiUrl = new URL(API_BASE_URL, window.location.origin);
  apiUrl.protocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  apiUrl.pathname = `${apiUrl.pathname.replace(/\/$/, "")}/contacts/ws`;
  return apiUrl.toString().replace(/\/$/, "");
}

export const domainClient = axios.create({
  baseURL: DOMAIN_BASE_URL,
  timeout: 8000,
  withCredentials: true,
});

export const analyticsClient = axios.create({
  baseURL: ANALYTICS_BASE_URL,
  timeout: 8000,
  withCredentials: true,
});

export const profileClient = axios.create({
  baseURL: PROFILE_BASE_URL,
  timeout: 8000,
  withCredentials: true,
});

[domainClient, analyticsClient, profileClient].forEach((client) => {
  client.interceptors.request.use((config) => {
    const token = getStoredAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });
});

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | undefined;
    return data?.message || data?.error || error.message;
  }
  return error instanceof Error ? error.message : "Unexpected error";
}
