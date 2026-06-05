<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { apiErrorMessage } from "@/api/client";
import { trustedRedirectUrl } from "@/infra/navigation/trustedRedirect";
import { useAuthStore } from "@/infra/store";
import AvatarCropper from "@/features/avatar/ui/AvatarCropper.vue";
import SessionsTab from "@/features/sessions/ui/SessionsTab.vue";
import AccountSwitchModal from "@/features/profile/ui/AccountSwitchModal.vue";
import { userDisplayName, userInitials } from "@/shared/lib/user";

type ProfileTab = "profile" | "sessions";
type MessageTone = "success" | "error" | "warning";
type EditableProfileField = "firstName" | "lastName" | "bio";

const authStore = useAuthStore();
const route = useRoute();
const { t } = useI18n();
const activeTab = ref<ProfileTab>("profile");
const profileMessage = ref("");
const messageTone = ref<MessageTone>("success");
const isSavingProfile = ref(false);
const isUploadingAvatar = ref(false);
const isAccountModalOpen = ref(false);
const cropFile = ref<File | null>(null);
const avatarPreviewUrl = ref<string | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);

const profileForm = reactive({
  firstName: "",
  lastName: "",
  bio: "",
});
const editingFields = reactive<Record<EditableProfileField, boolean>>({
  firstName: false,
  lastName: false,
  bio: false,
});

const displayName = computed(() => userDisplayName(authStore.currentUser));
const displayInitials = computed(() => userInitials(authStore.currentUser));
const avatarPreview = computed(() => avatarPreviewUrl.value || authStore.currentUser?.avatarUrl || "");
const backUrl = computed(() => trustedRedirectUrl(route.query.redirect));
const isProfileDirty = computed(() => {
  const user = authStore.currentUser;
  return (
    profileForm.firstName !== (user?.firstName || "") ||
    profileForm.lastName !== (user?.lastName || "") ||
    profileForm.bio !== (user?.bio || "")
  );
});
const editableProfileFields = computed<
  Array<{ key: EditableProfileField; label: string; type: "text" | "textarea"; autocomplete?: string }>
>(() => [
  { key: "firstName", label: t("auth.firstName"), type: "text", autocomplete: "given-name" },
  { key: "lastName", label: t("auth.lastName"), type: "text", autocomplete: "family-name" },
  { key: "bio", label: t("profile.bio"), type: "textarea" },
]);

const syncProfileForm = () => {
  profileForm.firstName = authStore.currentUser?.firstName || "";
  profileForm.lastName = authStore.currentUser?.lastName || "";
  profileForm.bio = authStore.currentUser?.bio || "";
};

watch(() => authStore.currentUser, syncProfileForm, { immediate: true });

onMounted(async () => {
  await authStore.fetchSessions().catch((cause) => {
    setMessage(apiErrorMessage(cause), "error");
  });
});

onBeforeUnmount(() => {
  revokeAvatarPreview();
});

function setMessage(message: string, tone: MessageTone = "success") {
  profileMessage.value = message;
  messageTone.value = tone;
}

const openFieldEdit = (field: EditableProfileField) => {
  editingFields[field] = true;
};

const confirmFieldEdit = async (field: EditableProfileField) => {
  if (!isProfileDirty.value) {
    editingFields[field] = false;
    return;
  }
  await saveProfile();
};

const closeFieldEdits = () => {
  editingFields.firstName = false;
  editingFields.lastName = false;
  editingFields.bio = false;
};

const saveProfile = async () => {
  isSavingProfile.value = true;
  profileMessage.value = "";
  try {
    await authStore.updateProfile({
      firstName: profileForm.firstName,
      lastName: profileForm.lastName,
      bio: profileForm.bio,
    });
    closeFieldEdits();
    setMessage(t("profile.profileUpdated"));
  } catch (cause) {
    setMessage(apiErrorMessage(cause), "error");
  } finally {
    isSavingProfile.value = false;
  }
};

const openAvatarPicker = () => {
  fileInput.value?.click();
};

const onAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] || null;
  input.value = "";
  if (!file) return;

  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    setMessage(t("profile.avatarTypeError"), "error");
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    setMessage(t("profile.avatarSizeError"), "error");
    return;
  }

  cropFile.value = file;
};

const uploadCroppedAvatar = async (file: File, previewUrl: string) => {
  revokeAvatarPreview();
  avatarPreviewUrl.value = previewUrl;
  cropFile.value = null;
  isUploadingAvatar.value = true;
  profileMessage.value = "";
  try {
    await authStore.uploadAvatar(file);
    setMessage(t("profile.avatarUploaded"));
  } catch (cause) {
    setMessage(apiErrorMessage(cause), "error");
  } finally {
    isUploadingAvatar.value = false;
  }
};

const revokeAvatarPreview = () => {
  if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value);
  avatarPreviewUrl.value = null;
};
</script>

<template>
  <main class="profile-page">
    <nav v-if="backUrl" class="profile-backbar" aria-label="External navigation">
      <a :href="backUrl" class="btn btn-ghost back-btn">
        <i class="pi pi-arrow-left"></i>
        {{ t("common.back") }}
      </a>
    </nav>

    <header class="profile-hero">
      <div class="identity-block">
        <button
          class="avatar-edit"
          type="button"
          :disabled="isUploadingAvatar"
          :aria-label="t('profile.changePhoto')"
          @click="openAvatarPicker"
        >
          <span class="avatar lg">
            <img v-if="avatarPreview" :src="avatarPreview" alt="" />
            <span v-else>{{ displayInitials }}</span>
          </span>
          <span class="avatar-edit-overlay" aria-hidden="true">
            <i :class="isUploadingAvatar ? 'pi pi-spinner pi-spin' : 'pi pi-camera'"></i>
          </span>
        </button>
        <input
          ref="fileInput"
          class="visually-hidden"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          @change="onAvatarChange"
        />
        <div>
          <h1>{{ displayName }}</h1>
          <p>{{ authStore.currentUser?.email }}</p>
        </div>
      </div>
      <button
        class="icon-button quiet account-switch-button"
        type="button"
        :aria-label="t('profile.switchAccount')"
        :title="t('profile.switchAccount')"
        @click="isAccountModalOpen = true"
      >
        <i class="pi pi-users"></i>
      </button>
    </header>

    <span v-if="profileMessage" class="status-badge" :class="messageTone">{{ profileMessage }}</span>

    <div class="profile-shell">
      <nav class="profile-icon-tabs" aria-label="Profile sections">
        <button
          type="button"
          :class="{ active: activeTab === 'profile' }"
          :aria-label="t('profile.profile')"
          :title="t('profile.profile')"
          @click="activeTab = 'profile'"
        >
          <i class="pi pi-user"></i>
          <span class="visually-hidden">{{ t("profile.profile") }}</span>
        </button>
        <button
          type="button"
          :class="{ active: activeTab === 'sessions' }"
          :aria-label="t('profile.sessions')"
          :title="t('profile.sessions')"
          @click="activeTab = 'sessions'"
        >
          <i class="pi pi-desktop"></i>
          <span class="visually-hidden">{{ t("profile.sessions") }}</span>
        </button>
      </nav>

      <div class="profile-stage">
        <section v-if="activeTab === 'profile'" class="tab-panel">
          <div class="section-toolbar">
            <h2>{{ t("profile.profile") }}</h2>
          </div>

          <form class="profile-form inline-profile-form" @submit.prevent>
            <div class="profile-detail-list" aria-label="Profile details">
              <article
                v-for="field in editableProfileFields"
                :key="field.key"
                class="profile-edit-row"
                :class="{ editing: editingFields[field.key] }"
              >
                <label class="profile-edit-label" :for="`profile-${field.key}`">{{ field.label }}</label>
                <div class="profile-edit-value">
                  <input
                    v-if="editingFields[field.key] && field.type === 'text'"
                    :id="`profile-${field.key}`"
                    v-model="profileForm[field.key]"
                    class="input inline-input"
                    :autocomplete="field.autocomplete"
                    autofocus
                  />
                  <textarea
                    v-else-if="editingFields[field.key]"
                    :id="`profile-${field.key}`"
                    v-model="profileForm[field.key]"
                    class="textarea inline-textarea"
                    maxlength="500"
                    rows="4"
                    autofocus
                  ></textarea>
                  <p v-else :class="{ muted: !profileForm[field.key] }">
                    {{ profileForm[field.key] || t("profile.notSpecified") }}
                  </p>
                </div>
                <button
                  class="icon-button quiet edit-field-button"
                  :disabled="isSavingProfile"
                  type="button"
                  :aria-label="editingFields[field.key] ? t('profile.finishEditing') : t('profile.editField')"
                  :title="editingFields[field.key] ? t('profile.finishEditing') : t('profile.editField')"
                  @click="editingFields[field.key] ? confirmFieldEdit(field.key) : openFieldEdit(field.key)"
                >
                  <i
                    :class="
                      isSavingProfile && editingFields[field.key]
                        ? 'pi pi-spinner pi-spin'
                        : editingFields[field.key]
                          ? 'pi pi-check'
                          : 'pi pi-pencil'
                    "
                  ></i>
                </button>
              </article>

              <article class="profile-edit-row readonly-row">
                <span class="profile-edit-label">{{ t("auth.username") }}</span>
                <div class="profile-edit-value">
                  <p>{{ authStore.currentUser?.username || t("common.unknown") }}</p>
                </div>
                <span class="readonly-marker">{{ t("profile.readonly") }}</span>
              </article>

              <article class="profile-edit-row readonly-row">
                <span class="profile-edit-label">{{ t("auth.email") }}</span>
                <div class="profile-edit-value">
                  <p>{{ authStore.currentUser?.email || t("common.unknown") }}</p>
                </div>
                <span :class="authStore.currentUser?.emailVerified ? 'readonly-marker text-success' : 'readonly-marker text-danger'">
                  {{ authStore.currentUser?.emailVerified ? t("common.verified") : t("common.notVerified") }}
                </span>
              </article>
            </div>
          </form>
        </section>

        <SessionsTab v-else @message="setMessage" />
      </div>
    </div>

    <AccountSwitchModal v-if="isAccountModalOpen" @close="isAccountModalOpen = false" />
    <AvatarCropper
      v-if="cropFile"
      :file="cropFile"
      @cancel="cropFile = null"
      @apply="uploadCroppedAvatar"
    />
  </main>
</template>
