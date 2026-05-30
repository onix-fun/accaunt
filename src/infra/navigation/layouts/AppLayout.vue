<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const backUrl = computed(() => {
    const redirect = route.query.redirect;
    if (!redirect) return null;
    const rawUrl = Array.isArray(redirect) ? redirect[0] : redirect;
    // Ensure it's decoded properly
    return rawUrl ? decodeURIComponent(String(rawUrl)) : null;
});

const goBack = () => {
    if (backUrl.value) {
        window.location.href = backUrl.value;
    }
};
</script>

<template>
    <div class="profile-layout">
        <div class="top-nav">
            <button v-if="backUrl" class="btn btn-ghost back-btn" type="button" aria-label="Back" @click="goBack">
                <i class="pi pi-arrow-left"></i>
                <span>Back</span>
            </button>
        </div>

        <main class="profile-content">
            <slot />
        </main>
    </div>
</template>

<style scoped>
.profile-layout {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

.top-nav {
    padding: 1.5rem;
    position: sticky;
    top: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    min-height: 72px;
}

.back-btn {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-weight: 500;
    color: var(--muted);
    font-size: 14px;
}

.back-btn i {
    font-size: 1rem;
}

.profile-content {
    flex: 1;
    padding: 1rem;
    display: flex;
    flex-direction: column;
}
</style>
