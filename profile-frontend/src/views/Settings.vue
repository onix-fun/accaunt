<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useAuthStore } from "@/infra/store";
import { apiErrorMessage } from "@/api/client";
import type { AuthSession } from "@/domain";

const authStore = useAuthStore();
const profileMessage = ref("");
const messageTone = ref<"success" | "error" | "warning">("success");
const avatarFile = ref<File | null>(null);
const avatarPreviewUrl = ref<string | null>(null);
const isSavingProfile = ref(false);
const isUploadingAvatar = ref(false);
const isLoadingSessions = ref(false);

const profileForm = reactive({
    email: "",
    firstName: "",
    lastName: "",
    bio: "",
});

const syncProfileForm = () => {
    profileForm.email = authStore.currentUser?.email || "";
    profileForm.firstName = authStore.currentUser?.firstName || "";
    profileForm.lastName = authStore.currentUser?.lastName || "";
    profileForm.bio = authStore.currentUser?.bio || "";
};

const displayInitials = computed(() => {
    return displayInitialsFor(authStore.currentUser);
});

const displayInitialsFor = (user: any) => {
    if (!user) return "S";
    const source = [user.firstName, user.lastName].filter(Boolean).join(" ") || user.username || "S";
    return source
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part: string) => part[0]?.toUpperCase())
        .join("");
};

const avatarPreview = computed(() => avatarPreviewUrl.value || authStore.currentUser?.avatarUrl || "");

watch(() => authStore.currentUser, syncProfileForm, { immediate: true });

onMounted(async () => {
    await refreshSessions();
});

onBeforeUnmount(() => {
    revokeAvatarPreview();
});

const setMessage = (message: string, tone: "success" | "error" | "warning" = "success") => {
    profileMessage.value = message;
    messageTone.value = tone;
};

const saveProfile = async () => {
    profileMessage.value = "";
    isSavingProfile.value = true;
    try {
        await authStore.updateProfile({
            email: profileForm.email,
            firstName: profileForm.firstName,
            lastName: profileForm.lastName,
            bio: profileForm.bio,
        });
        setMessage("Profile updated");
    } catch (cause) {
        setMessage(apiErrorMessage(cause), "error");
    } finally {
        isSavingProfile.value = false;
    }
};

const uploadAvatar = async () => {
    if (!avatarFile.value) return;
    profileMessage.value = "";
    isUploadingAvatar.value = true;
    try {
        await authStore.uploadAvatar(avatarFile.value);
        clearAvatarSelection();
        setMessage("Avatar uploaded");
    } catch (cause) {
        setMessage(apiErrorMessage(cause), "error");
    } finally {
        isUploadingAvatar.value = false;
    }
};

const refreshSessions = async () => {
    isLoadingSessions.value = true;
    try {
        await authStore.fetchSessions();
    } catch (cause) {
        setMessage(apiErrorMessage(cause), "error");
    } finally {
        isLoadingSessions.value = false;
    }
};

const revokeSession = async (session: AuthSession) => {
    try {
        await authStore.revokeSession(session.id);
        setMessage("Session revoked");
    } catch (cause) {
        setMessage(apiErrorMessage(cause), "error");
    }
};

const logoutAll = async () => {
    try {
        await authStore.logoutAll();
    } catch (cause) {
        setMessage(apiErrorMessage(cause), "error");
    }
};

const onAvatarChange = (event: Event) => {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;
    revokeAvatarPreview();
    avatarFile.value = null;

    if (!file) return;
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
        input.value = "";
        setMessage("Avatar must be a JPEG, PNG, or WebP image", "error");
        return;
    }
    if (file.size > 5 * 1024 * 1024) {
        input.value = "";
        setMessage("Avatar must be 5MB or smaller", "error");
        return;
    }

    avatarFile.value = file;
    avatarPreviewUrl.value = URL.createObjectURL(file);
    profileMessage.value = "";
    uploadAvatar();
};

const clearAvatarSelection = () => {
    avatarFile.value = null;
    revokeAvatarPreview();
};

const revokeAvatarPreview = () => {
    if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value);
    avatarPreviewUrl.value = null;
};

const switchAccount = async (userId: string) => {
    await authStore.switchAccount(userId);
};

const addAccount = () => {
    authStore.promptAddAccount();
};

const sessionDevice = (session: AuthSession) => session.deviceId || session.userAgent || "Unknown device";
const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : "Unknown");
</script>

<template>
    <div class="settings-page">
        <span v-if="profileMessage" class="status-badge" :class="messageTone" style="margin-bottom: 1rem">{{
            profileMessage
        }}</span>

        <section class="settings-section">
            <div class="section-head">
                <h4>Accounts</h4>
            </div>
            <div class="session-list">
                <div
                    v-for="acc in authStore.storedAccounts"
                    :key="acc.id"
                    class="session-row"
                    :class="{ 'is-current': acc.id === authStore.currentUser?.id }"
                >
                    <div class="session-info">
                        <div class="avatar-circle" style="width: 32px; height: 32px; font-size: 12px; margin: 0">
                            <img v-if="acc.avatarUrl" :src="acc.avatarUrl" alt="" />
                            <span v-else>{{ displayInitialsFor(acc) }}</span>
                        </div>
                        <div>
                            <div class="session-device">{{ acc.username }}</div>
                            <span class="session-meta">{{ acc.email }}</span>
                        </div>
                    </div>
                    <button
                        v-if="acc.id !== authStore.currentUser?.id"
                        class="btn btn-ghost"
                        type="button"
                        @click="switchAccount(acc.id)"
                    >
                        Switch
                    </button>
                </div>
                <button class="session-row add-account-btn" type="button" @click="addAccount">
                    <i class="pi pi-plus"></i> Add Account
                </button>
            </div>
        </section>

        <section class="settings-section">
            <div class="profile-head">
                <div class="avatar-circle" aria-label="Avatar preview">
                    <img v-if="avatarPreview" :src="avatarPreview" alt="" />
                    <span v-else>{{ displayInitials }}</span>
                </div>
                <label class="btn btn-ghost" for="avatar-input">
                    <i class="pi pi-image"></i>
                    Change photo
                </label>
                <input
                    id="avatar-input"
                    class="visually-hidden"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    @change="onAvatarChange"
                />
                <span class="muted avatar-hint">JPEG, PNG, WebP up to 5MB</span>
            </div>

            <form class="profile-form" @submit.prevent="saveProfile">
                <div class="field-row">
                    <label class="field">
                        <span>Username</span>
                        <input :value="authStore.currentUser?.username" class="input" readonly />
                    </label>
                </div>
                <div class="field-row">
                    <label class="field">
                        <span>Email</span>
                        <input v-model="profileForm.email" class="input" type="email" autocomplete="email" readonly />
                    </label>
                </div>
                <div class="field-row cols-2">
                    <label class="field">
                        <span>First name</span>
                        <input v-model="profileForm.firstName" class="input" autocomplete="given-name" />
                    </label>
                    <label class="field">
                        <span>Last name</span>
                        <input v-model="profileForm.lastName" class="input" autocomplete="family-name" />
                    </label>
                </div>
                <label class="field">
                    <span>Bio</span>
                    <textarea v-model="profileForm.bio" class="textarea" maxlength="500" rows="3"></textarea>
                </label>
                <div class="field-actions">
                    <button class="btn btn-primary" type="submit" :disabled="isSavingProfile" style="width: 100%">
                        <i class="pi pi-save"></i>
                        Save
                    </button>
                </div>
            </form>
        </section>

        <section class="settings-section">
            <div class="section-head">
                <h4>Sessions</h4>
                <div class="section-actions">
                    <button class="btn btn-ghost" type="button" :disabled="isLoadingSessions" @click="refreshSessions">
                        <i class="pi pi-refresh"></i>
                        Refresh
                    </button>
                    <button class="btn btn-ghost btn-ghost-danger" type="button" @click="logoutAll">
                        <i class="pi pi-sign-out"></i>
                        Logout all
                    </button>
                </div>
            </div>
            <div class="session-list">
                <div v-if="!authStore.sessions.length" class="empty-state">No active sessions</div>
                <div
                    v-for="session in authStore.sessions"
                    :key="session.id"
                    class="session-row"
                    :class="{ 'is-current': session.isCurrent }"
                >
                    <div class="session-info">
                        <i class="pi pi-desktop"></i>
                        <div>
                            <div class="session-device">{{ sessionDevice(session) }}</div>
                            <span class="session-meta"
                                >{{ session.ipAddress || "Unknown" }} &middot;
                                {{ formatDate(session.lastUsedAt) }}</span
                            >
                        </div>
                    </div>
                    <div class="session-expiry">expires {{ formatDate(session.expiresAt) }}</div>
                    <button
                        v-if="session.isCurrent"
                        class="btn btn-ghost btn-ghost-danger"
                        type="button"
                        @click="authStore.logout()"
                    >
                        <i class="pi pi-sign-out"></i>
                        Log out
                    </button>
                    <button v-else class="btn btn-ghost btn-ghost-danger" type="button" @click="revokeSession(session)">
                        <i class="pi pi-ban"></i>
                        Revoke
                    </button>
                </div>
            </div>
        </section>
    </div>
</template>

<style scoped>
.settings-page {
    max-width: 480px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 2rem;
    padding-bottom: 3rem;
}

.settings-section {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.profile-head {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 0.5rem;
}

.avatar-circle {
    width: 88px;
    height: 88px;
    border-radius: 50%;
    background: var(--surface-muted);
    border: 1px solid var(--border);
    color: var(--text);
    display: grid;
    place-items: center;
    overflow: hidden;
    font-size: 28px;
    font-weight: 700;
    flex-shrink: 0;
}

.avatar-circle img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.avatar-hint {
    font-size: 11px;
}

.profile-form {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.profile-form input[readonly] {
    background: transparent;
    border-color: transparent;
    padding-left: 0;
    color: var(--text);
    cursor: default;
    font-weight: 600;
}

.profile-form input[readonly]:focus,
.profile-form input[readonly]:active {
    outline: none;
    box-shadow: none;
}

.field-row {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.field-row.cols-2 {
    flex-direction: row;
}

.field-row.cols-2 .field {
    flex: 1;
}

.field-actions {
    display: flex;
    gap: 0.5rem;
    padding-top: 0.25rem;
}

.section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    margin-bottom: 0.75rem;
}

.section-head h4 {
    margin: 0;
    font-size: 13px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--muted);
}

.section-actions {
    display: flex;
    gap: 0.5rem;
}

.session-list {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 1px;
    background: var(--border);
    border: 1px solid var(--border);
    border-radius: 8px;
    overflow: hidden;
}

.session-row.is-current {
    background: var(--surface-muted);
}

.session-row.is-current .session-device::after {
    content: " (current)";
    font-weight: 400;
    color: var(--muted);
}

.session-row {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    background: var(--surface);
}

.session-info {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex: 1;
    min-width: 0;
}

.session-info i {
    font-size: 1.1rem;
    color: var(--muted);
}

.session-device {
    font-weight: 600;
    font-size: 13px;
}

.session-meta {
    font-size: 12px;
    color: var(--muted);
}

.session-expiry {
    font-size: 12px;
    color: var(--subtle);
    white-space: nowrap;
}

.add-account-btn {
    width: 100%;
    justify-content: center;
    color: var(--primary);
    font-weight: 500;
    cursor: pointer;
    border: none;
    font-family: inherit;
}
.add-account-btn:hover {
    background: var(--surface-muted);
}

.empty-state {
    padding: 2rem 1rem;
    text-align: center;
    color: var(--muted);
    background: var(--surface);
    font-size: 13px;
}

.btn-ghost {
    border-color: transparent;
    background: transparent;
    min-height: 32px;
    padding: 0 0.5rem;
    color: var(--muted);
}

.btn-ghost:hover {
    background: var(--surface-muted);
    color: var(--text);
}

.btn-ghost-danger:hover {
    color: var(--danger);
    background: #fef2f2;
}
</style>
