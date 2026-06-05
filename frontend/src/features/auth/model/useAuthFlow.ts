import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { AuthService } from "@/api/services/AuthService";
import { trustedRedirectUrl } from "@/infra/navigation/trustedRedirect";
import { useAuthStore } from "@/infra/store";

export type AuthMode = "identifier" | "password" | "register" | "confirm" | "name" | "forgot" | "reset";

export function useAuthFlow() {
  const route = useRoute();
  const router = useRouter();
  const authStore = useAuthStore();
  const { t } = useI18n();

  const mode = ref<AuthMode>(authStore.isCompletingRegistrationProfile ? "name" : "identifier");
  const authMessage = ref("");
  const loginIdentifier = ref("");
  const loginPassword = ref("");
  const registerForm = ref({
    email: "",
    username: "",
    password: "",
    confirmPassword: "",
  });
  const nameForm = ref({
    firstName: "",
    lastName: "",
  });
  const pendingRegistrationEmail = ref("");
  const registrationCode = ref("");
  const forgotIdentifier = ref("");
  const resetIdentifier = ref("");
  const resetCode = ref("");
  const resetPassword = ref("");
  const isCheckingUsername = ref(false);
  const isUsernameTaken = ref(false);
  const usernameCheckTouched = ref(false);
  let usernameTimeout: ReturnType<typeof setTimeout> | null = null;

  const activeStep = computed(() => {
    if (mode.value === "password" || mode.value === "forgot" || mode.value === "reset") return "password";
    if (mode.value === "confirm") return "code";
    if (mode.value === "name") return "done";
    return "account";
  });

  const showRegistrationSteps = computed(() => {
    return mode.value === "register" || mode.value === "confirm" || mode.value === "name";
  });

  const registerPasswordMismatch = computed(() => {
    return Boolean(registerForm.value.confirmPassword) && registerForm.value.password !== registerForm.value.confirmPassword;
  });

  const title = computed(() => {
    if (mode.value === "register") return t("auth.registerTitle");
    if (mode.value === "confirm") return t("auth.confirmTitle");
    if (mode.value === "name") return t("auth.nameTitle");
    if (mode.value === "forgot") return t("auth.forgotTitle");
    if (mode.value === "reset") return t("auth.resetTitle");
    return t("auth.signIn");
  });

  watch(
    () => registerForm.value.username,
    (newVal) => {
      usernameCheckTouched.value = true;
      isCheckingUsername.value = false;
      isUsernameTaken.value = false;

      if (usernameTimeout) clearTimeout(usernameTimeout);

      const username = newVal.trim();
      if (!username) {
        usernameCheckTouched.value = false;
        return;
      }

      isCheckingUsername.value = true;
      usernameTimeout = setTimeout(async () => {
        try {
          isUsernameTaken.value = !(await AuthService.isUsernameAvailable(username));
        } catch {
          isUsernameTaken.value = false;
        } finally {
          isCheckingUsername.value = false;
        }
      }, 450);
    },
  );

  const handleAuthSuccess = async () => {
    const target = trustedRedirectUrl(route.query.redirect);
    if (target) {
      window.location.href = target;
      return;
    }
    await router.push("/");
  };

  const showIdentifierStep = () => {
    authMessage.value = "";
    mode.value = "identifier";
  };

  const continueToPassword = () => {
    authMessage.value = "";
    if (!loginIdentifier.value.trim()) return;
    mode.value = "password";
  };

  const login = async () => {
    authMessage.value = "";
    try {
      await authStore.login(loginIdentifier.value, loginPassword.value);
      await handleAuthSuccess();
    } catch {
      authMessage.value = authStore.error || t("auth.loginFailed");
    }
  };

  const register = async () => {
    authMessage.value = "";
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      authMessage.value = t("auth.passwordMismatch");
      return;
    }
    try {
      const response = await authStore.register({
        email: registerForm.value.email,
        username: registerForm.value.username,
        password: registerForm.value.password,
      });
      pendingRegistrationEmail.value = response.email;
      registrationCode.value = "";
      mode.value = "confirm";
      authMessage.value = t("auth.verificationSent");
    } catch {
      authMessage.value = authStore.error || t("auth.registrationFailed");
    }
  };

  const confirmRegistration = async () => {
    authMessage.value = "";
    try {
      await authStore.confirmRegistration(pendingRegistrationEmail.value, registrationCode.value);
      nameForm.value = { firstName: "", lastName: "" };
      mode.value = "name";
      authMessage.value = t("auth.emailConfirmed");
    } catch {
      authMessage.value = authStore.error || t("auth.confirmationFailed");
    }
  };

  const completeNameStep = async () => {
    authMessage.value = "";
    try {
      await authStore.completeRegistrationProfile(nameForm.value);
      await handleAuthSuccess();
    } catch {
      authMessage.value = authStore.error || t("auth.profileFailed");
    }
  };

  const resendRegistrationCode = async () => {
    authMessage.value = "";
    try {
      const response = await authStore.resendRegistrationCode(
        pendingRegistrationEmail.value || registerForm.value.email,
      );
      pendingRegistrationEmail.value = response.email;
      authMessage.value = t("auth.verificationResent");
    } catch {
      authMessage.value = authStore.error || t("auth.confirmationFailed");
    }
  };

  const forgotPassword = async () => {
    authMessage.value = "";
    try {
      await authStore.forgotPassword(forgotIdentifier.value);
      resetIdentifier.value = forgotIdentifier.value;
      resetCode.value = "";
      resetPassword.value = "";
      authMessage.value = t("auth.resetSent");
      mode.value = "reset";
    } catch {
      authMessage.value = authStore.error || t("auth.resetFailed");
    }
  };

  const submitResetPassword = async () => {
    authMessage.value = "";
    try {
      await authStore.resetPassword(resetIdentifier.value, resetCode.value, resetPassword.value);
      mode.value = "identifier";
      authMessage.value = t("auth.passwordUpdated");
    } catch {
      authMessage.value = authStore.error || t("auth.resetFailed");
    }
  };

  return {
    activeStep,
    authMessage,
    completeNameStep,
    confirmRegistration,
    continueToPassword,
    forgotIdentifier,
    forgotPassword,
    isCheckingUsername,
    isUsernameTaken,
    login,
    loginIdentifier,
    loginPassword,
    mode,
    nameForm,
    pendingRegistrationEmail,
    register,
    registerForm,
    registerPasswordMismatch,
    registrationCode,
    resendRegistrationCode,
    resetCode,
    resetIdentifier,
    resetPassword,
    showIdentifierStep,
    showRegistrationSteps,
    submitResetPassword,
    title,
    usernameCheckTouched,
  };
}
