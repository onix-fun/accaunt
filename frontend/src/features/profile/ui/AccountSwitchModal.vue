<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/infra/store";
import { userDisplayName, userInitials } from "@/shared/lib/user";

const emit = defineEmits<{
  close: [];
}>();

const authStore = useAuthStore();
const { t } = useI18n();

const switchAccount = async (userId: string) => {
  await authStore.switchAccount(userId);
  emit("close");
};

const addAccount = () => {
  authStore.promptAddAccount();
  emit("close");
};

const logoutAll = async () => {
  await authStore.logoutAll();
  emit("close");
};
</script>

<template>
  <div class="modal-backdrop minimal" @click.self="emit('close')">
    <section class="account-modal" role="dialog" aria-modal="true" :aria-label="t('profile.accountsTitle')">
      <header class="modal-head">
        <h2>{{ t("profile.accountsTitle") }}</h2>
        <button class="icon-button quiet" type="button" :aria-label="t('common.cancel')" @click="emit('close')">
          <i class="pi pi-times"></i>
        </button>
      </header>

      <div class="account-list">
        <button
          v-for="account in authStore.storedAccounts"
          :key="account.id"
          class="account-row"
          :class="{ selected: account.id === authStore.currentUser?.id }"
          type="button"
          @click="account.id === authStore.currentUser?.id ? emit('close') : switchAccount(account.id)"
        >
          <span class="avatar sm">
            <img v-if="account.avatarUrl" :src="account.avatarUrl" alt="" />
            <span v-else>{{ userInitials(account) }}</span>
          </span>
          <span class="account-copy">
            <strong>{{ userDisplayName(account) }}</strong>
            <small>{{ account.email }}</small>
          </span>
          <i v-if="account.id === authStore.currentUser?.id" class="pi pi-check"></i>
        </button>

        <button class="account-row action-row" type="button" @click="addAccount">
          <span class="action-icon"><i class="pi pi-plus"></i></span>
          <span class="account-copy">
            <strong>{{ t("profile.addAccount") }}</strong>
          </span>
        </button>
      </div>

      <footer class="modal-foot">
        <button class="link-button danger" type="button" @click="logoutAll">
          {{ t("profile.signOutAll") }}
        </button>
      </footer>
    </section>
  </div>
</template>
