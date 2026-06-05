<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/infra/store";
import { useAuthFlow } from "@/features/auth/model/useAuthFlow";
import LocaleSwitcher from "@/shared/ui/LocaleSwitcher.vue";

const authStore = useAuthStore();
const { t } = useI18n();
const flow = useAuthFlow();

const steps = [
  { key: "account", label: "auth.steps.account" },
  { key: "password", label: "auth.steps.password" },
  { key: "code", label: "auth.steps.code" },
  { key: "done", label: "auth.steps.done" },
];
</script>

<template>
  <section class="auth-screen">
    <main class="auth-flow" aria-live="polite">
      <nav v-if="flow.showRegistrationSteps.value" class="auth-stepper" aria-label="Authentication steps">
        <span
          v-for="step in steps"
          :key="step.key"
          :class="{ active: flow.activeStep.value === step.key }"
        >
          {{ t(step.label) }}
        </span>
      </nav>

      <div class="auth-title-row">
        <button
          v-if="flow.mode.value !== 'identifier' && flow.mode.value !== 'name'"
          class="icon-button quiet"
          type="button"
          :aria-label="t('common.back')"
          @click="flow.showIdentifierStep"
        >
          <i class="pi pi-arrow-left"></i>
        </button>
        <h1>{{ flow.title.value }}</h1>
      </div>

      <form v-if="flow.mode.value === 'identifier'" class="account-form" @submit.prevent="flow.continueToPassword">
        <label class="field">
          <span>{{ t("auth.emailOrUsername") }}</span>
          <input v-model="flow.loginIdentifier.value" class="input xl" autocomplete="username" required autofocus />
        </label>
        <div class="auth-actions split">
          <button class="link-button" type="button" @click="flow.mode.value = 'register'">
            {{ t("auth.createAccount") }}
          </button>
          <button class="btn btn-primary" type="submit">{{ t("common.continue") }}</button>
        </div>
        <button class="link-button subtle-link" type="button" @click="flow.mode.value = 'forgot'">
          {{ t("auth.forgotPassword") }}
        </button>
      </form>

      <form v-else-if="flow.mode.value === 'password'" class="account-form" @submit.prevent="flow.login">
        <div class="account-chip">
          <i class="pi pi-user"></i>
          <span>{{ flow.loginIdentifier.value }}</span>
        </div>
        <label class="field">
          <span>{{ t("auth.password") }}</span>
          <input
            v-model="flow.loginPassword.value"
            class="input xl"
            type="password"
            autocomplete="current-password"
            required
            autofocus
          />
        </label>
        <div class="auth-actions split">
          <button class="link-button" type="button" @click="flow.mode.value = 'forgot'">
            {{ t("auth.forgotPassword") }}
          </button>
          <button class="btn btn-primary" type="submit" :disabled="authStore.isLoading">
            {{ t("auth.signIn") }}
          </button>
        </div>
      </form>

      <form v-else-if="flow.mode.value === 'register'" class="account-form" @submit.prevent="flow.register">
        <label class="field">
          <span>{{ t("auth.username") }}</span>
          <div class="input-wrapper">
            <input v-model="flow.registerForm.value.username" class="input xl" autocomplete="username" required />
            <span v-if="flow.isCheckingUsername.value" class="input-suffix">
              <i class="pi pi-spinner pi-spin"></i>
            </span>
            <span
              v-else-if="flow.usernameCheckTouched.value && flow.registerForm.value.username"
              class="input-suffix"
              :class="flow.isUsernameTaken.value ? 'text-danger' : 'text-success'"
            >
              <i :class="flow.isUsernameTaken.value ? 'pi pi-times' : 'pi pi-check'"></i>
            </span>
          </div>
          <span
            v-if="flow.usernameCheckTouched.value && flow.registerForm.value.username && flow.isUsernameTaken.value"
            class="validation-message text-danger"
          >
            {{ t("auth.usernameTaken") }}
          </span>
          <span
            v-else-if="
              flow.usernameCheckTouched.value &&
              flow.registerForm.value.username &&
              !flow.isCheckingUsername.value &&
              !flow.isUsernameTaken.value
            "
            class="validation-message text-success"
          >
            {{ t("auth.usernameAvailable") }}
          </span>
        </label>
        <label class="field">
          <span>{{ t("auth.email") }}</span>
          <input v-model="flow.registerForm.value.email" class="input xl" type="email" autocomplete="email" required />
        </label>
        <label class="field">
          <span>{{ t("auth.password") }}</span>
          <input
            v-model="flow.registerForm.value.password"
            class="input xl"
            type="password"
            autocomplete="new-password"
            required
            minlength="8"
          />
        </label>
        <label class="field">
          <span>{{ t("auth.confirmPassword") }}</span>
          <input
            v-model="flow.registerForm.value.confirmPassword"
            class="input xl"
            type="password"
            autocomplete="new-password"
            required
            minlength="8"
            :aria-invalid="flow.registerPasswordMismatch.value"
          />
          <span v-if="flow.registerPasswordMismatch.value" class="validation-message text-danger">
            {{ t("auth.passwordMismatch") }}
          </span>
        </label>
        <div class="auth-actions split">
          <button class="link-button" type="button" @click="flow.showIdentifierStep">
            {{ t("auth.backToSignIn") }}
          </button>
          <button
            class="btn btn-primary"
            type="submit"
            :disabled="
              authStore.isLoading ||
              flow.isCheckingUsername.value ||
              flow.isUsernameTaken.value ||
              !flow.registerForm.value.username ||
              !flow.registerForm.value.password ||
              !flow.registerForm.value.confirmPassword ||
              flow.registerPasswordMismatch.value
            "
          >
            {{ t("auth.sendCode") }}
          </button>
        </div>
      </form>

      <form v-else-if="flow.mode.value === 'confirm'" class="account-form" @submit.prevent="flow.confirmRegistration">
        <div class="account-chip">
          <i class="pi pi-envelope"></i>
          <span>{{ flow.pendingRegistrationEmail.value }}</span>
        </div>
        <label class="field">
          <span>{{ t("auth.verificationCode") }}</span>
          <input
            v-model="flow.registrationCode.value"
            class="input xl code-input"
            inputmode="numeric"
            pattern="[0-9]{6}"
            autocomplete="one-time-code"
            required
          />
        </label>
        <button class="btn btn-primary full" type="submit" :disabled="authStore.isLoading">
          {{ t("auth.confirmEmail") }}
        </button>
        <div class="auth-actions center">
          <button class="link-button" type="button" :disabled="authStore.isLoading" @click="flow.resendRegistrationCode">
            {{ t("auth.resendCode") }}
          </button>
          <button class="link-button" type="button" @click="flow.mode.value = 'register'">
            {{ t("auth.editRegistration") }}
          </button>
        </div>
      </form>

      <form v-else-if="flow.mode.value === 'name'" class="account-form" @submit.prevent="flow.completeNameStep">
        <div class="account-chip">
          <i class="pi pi-check-circle"></i>
          <span>{{ authStore.currentUser?.email }}</span>
        </div>
        <label class="field">
          <span>{{ t("auth.firstName") }}</span>
          <input v-model="flow.nameForm.value.firstName" class="input xl" autocomplete="given-name" required />
        </label>
        <label class="field">
          <span>{{ t("auth.lastName") }}</span>
          <input v-model="flow.nameForm.value.lastName" class="input xl" autocomplete="family-name" required />
        </label>
        <button class="btn btn-primary full" type="submit" :disabled="authStore.isLoading">
          {{ t("common.continue") }}
        </button>
      </form>

      <form v-else-if="flow.mode.value === 'forgot'" class="account-form" @submit.prevent="flow.forgotPassword">
        <label class="field">
          <span>{{ t("auth.emailOrUsername") }}</span>
          <input v-model="flow.forgotIdentifier.value" class="input xl" autocomplete="username" required autofocus />
        </label>
        <button class="btn btn-primary full" type="submit" :disabled="authStore.isLoading">
          {{ t("auth.sendCode") }}
        </button>
      </form>

      <form v-else class="account-form" @submit.prevent="flow.submitResetPassword">
        <label class="field">
          <span>{{ t("auth.emailOrUsername") }}</span>
          <input v-model="flow.resetIdentifier.value" class="input xl" autocomplete="username" required />
        </label>
        <label class="field">
          <span>{{ t("auth.verificationCode") }}</span>
          <input
            v-model="flow.resetCode.value"
            class="input xl code-input"
            inputmode="numeric"
            pattern="[0-9]{6}"
            autocomplete="one-time-code"
            required
          />
        </label>
        <label class="field">
          <span>{{ t("auth.newPassword") }}</span>
          <input
            v-model="flow.resetPassword.value"
            class="input xl"
            type="password"
            autocomplete="new-password"
            required
            minlength="8"
          />
        </label>
        <button class="btn btn-primary full" type="submit" :disabled="authStore.isLoading">
          {{ t("auth.resetPassword") }}
        </button>
      </form>

      <p v-if="flow.authMessage.value" class="form-message">{{ flow.authMessage.value }}</p>
    </main>

    <LocaleSwitcher class="auth-locale" />
  </section>
</template>
