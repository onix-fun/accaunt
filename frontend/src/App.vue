<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useAuthStore } from "@/infra/store";
import AuthScreen from "@/features/auth/ui/AuthScreen.vue";

const authStore = useAuthStore();
const isBooting = ref(true);

onMounted(async () => {
  try {
    await authStore.initAuth();
    if (authStore.isAuthenticated) {
      await authStore.fetchSessions().catch(() => undefined);
    }
  } finally {
    isBooting.value = false;
  }
});
</script>

<template>
  <div v-if="isBooting" class="boot-screen">
    <div class="spinner" aria-hidden="true"></div>
    <span>{{ $t("common.loading") }}</span>
  </div>

  <AuthScreen v-else-if="!authStore.isAuthenticated || authStore.isCompletingRegistrationProfile" />

  <router-view v-else />
</template>
