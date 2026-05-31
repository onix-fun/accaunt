import axios from "axios";

export const APP_API_BASE_URL = (
  import.meta.env.VITE_APP_API_URL ||
  import.meta.env.VITE_API_URL ||
  (import.meta.env.DEV ? "http://localhost:8088/api" : "/api")
).replace(/\/$/, "");
export const PROFILE_BASE_URL = "/api";
export const DOMAIN_BASE_URL = `${APP_API_BASE_URL}/domain`;
export const ANALYTICS_BASE_URL = `${APP_API_BASE_URL}/analytics`;

export function contactsWsBaseUrl(): string {
  const configured = import.meta.env.VITE_CONTACTS_WS_URL as string | undefined;
  if (configured) return configured.replace(/\/$/, "");

  const apiUrl = new URL(APP_API_BASE_URL, window.location.origin);
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

let csrfToken: string | null = null;
let csrfRequest: Promise<string> | null = null;
let sessionRefreshRequest: Promise<void> | null = null;

function isUnsafeMethod(method?: string): boolean {
  return ["post", "put", "patch", "delete"].includes((method || "").toLowerCase());
}

export async function initializeCsrfToken(): Promise<string> {
  if (csrfToken) return csrfToken;
  if (!csrfRequest) {
    csrfRequest = profileClient
      .get<{ csrfToken: string }>("/auth/csrf")
      .then((response) => {
        csrfToken = response.data.csrfToken;
        return csrfToken;
      })
      .finally(() => {
        csrfRequest = null;
      });
  }
  return csrfRequest;
}

export async function refreshBrowserSession(): Promise<void> {
  if (!sessionRefreshRequest) {
    sessionRefreshRequest = profileClient
      .post("/auth/refresh")
      .then(() => undefined)
      .finally(() => {
        sessionRefreshRequest = null;
      });
  }
  return sessionRefreshRequest;
}

[domainClient, analyticsClient, profileClient].forEach((client) => {
  client.interceptors.request.use(async (config) => {
    if (isUnsafeMethod(config.method) && !config.url?.startsWith("/auth/token")) {
      config.headers.set("X-CSRF-Token", await initializeCsrfToken());
    }
    return config;
  });

  client.interceptors.response.use(
    (response) => response,
    async (error) => {
      const config = error.config as (typeof error.config & { _sessionRetry?: boolean }) | undefined;
      const isAuthRequest = config?.url?.startsWith("/auth/");
      if (error.response?.status !== 401 || !config || config._sessionRetry || isAuthRequest) {
        return Promise.reject(error);
      }
      config._sessionRetry = true;
      await refreshBrowserSession();
      return client.request(config);
    },
  );
});

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | undefined;
    return data?.message || data?.error || error.message;
  }
  return error instanceof Error ? error.message : "Unexpected error";
}
