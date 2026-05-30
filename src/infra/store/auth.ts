import { computed, ref } from "vue";
import { defineStore } from "pinia";
import type { AuthSession, User } from "@/domain";
import { AuthService, type RegistrationStartedResponse } from "@/api/services/AuthService";
import { apiErrorMessage } from "@/api/client";

export const useAuthStore = defineStore("auth", () => {
  const currentUser = ref<User | null>(AuthService.getStoredSession());
  const sessions = ref<AuthSession[]>([]);
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const isAuthenticated = computed(() => Boolean(currentUser.value));
  const displayName = computed(() => {
    const user = currentUser.value;
    if (!user) return "";
    return [user.firstName, user.lastName].filter(Boolean).join(" ") || user.username;
  });

  const initAuth = async () => {
    error.value = null;

    isLoading.value = true;
    try {
      currentUser.value = await AuthService.refresh();
    } finally {
      isLoading.value = false;
    }
  };

  const login = async (identifier: string, password: string) => {
    isLoading.value = true;
    error.value = null;
    try {
      currentUser.value = await AuthService.login({ identifier, password });
    } catch (cause) {
      error.value = apiErrorMessage(cause);
      throw cause;
    } finally {
      isLoading.value = false;
    }
  };

  const register = async (payload: {
    email: string;
    username: string;
    password: string;
  }): Promise<RegistrationStartedResponse> => {
    isLoading.value = true;
    error.value = null;
    try {
      return await AuthService.register(payload);
    } catch (cause) {
      error.value = apiErrorMessage(cause);
      throw cause;
    } finally {
      isLoading.value = false;
    }
  };

  const confirmRegistration = async (email: string, code: string) => {
    isLoading.value = true;
    error.value = null;
    try {
      currentUser.value = await AuthService.confirmRegistration(email, code);
      return currentUser.value;
    } catch (cause) {
      error.value = apiErrorMessage(cause);
      throw cause;
    } finally {
      isLoading.value = false;
    }
  };

  const resendRegistrationCode = async (email: string) => {
    isLoading.value = true;
    error.value = null;
    try {
      return await AuthService.resendRegistrationCode(email);
    } catch (cause) {
      error.value = apiErrorMessage(cause);
      throw cause;
    } finally {
      isLoading.value = false;
    }
  };

  const updateProfile = async (payload: { email?: string; firstName?: string; lastName?: string; bio?: string }) => {
    currentUser.value = await AuthService.updateProfile(payload);
  };

  const uploadAvatar = async (file: File) => {
    currentUser.value = await AuthService.uploadAvatar(file);
  };

  const verifyEmail = async (code: string) => {
    await AuthService.verifyEmail(code);
    currentUser.value = await AuthService.getMe();
  };

  const resendVerification = async () => {
    await AuthService.resendVerification();
  };

  const forgotPassword = async (identifier: string) => {
    await AuthService.forgotPassword(identifier);
  };

  const resetPassword = async (identifier: string, code: string, newPassword: string) => {
    await AuthService.resetPassword(identifier, code, newPassword);
  };

  const fetchSessions = async () => {
    sessions.value = await AuthService.getSessions();
  };

  const revokeSession = async (id: string) => {
    await AuthService.revokeSession(id);
    sessions.value = sessions.value.filter((session) => session.id !== id);
  };

  const logout = async () => {
    await AuthService.logout();
    currentUser.value = null;
    sessions.value = [];
  };

  const logoutAll = async () => {
    await AuthService.logoutAll();
    currentUser.value = null;
    sessions.value = [];
  };

  const refreshMe = async () => {
    try {
      currentUser.value = await AuthService.getMe();
    } catch (cause) {
      error.value = apiErrorMessage(cause);
    } finally {
      isLoading.value = false;
    }
  };

  return {
    currentUser,
    sessions,
    isAuthenticated,
    displayName,
    isLoading,
    error,
    initAuth,
    login,
    register,
    confirmRegistration,
    resendRegistrationCode,
    updateProfile,
    uploadAvatar,
    verifyEmail,
    resendVerification,
    forgotPassword,
    resetPassword,
    fetchSessions,
    revokeSession,
    logout,
    logoutAll,
    refreshMe,
  };
});
