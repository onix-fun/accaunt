<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ContactsSocket } from "@/api/services/ContactsService";
import { apiErrorMessage } from "@/api/client";
import { useAnalyticsStore, useDeviceStore, useTemplateStore } from "@/infra/store";
import { toCommandContracts } from "@/domain";
import type { CommandContract, DomainType, LiveEvent, VariableResponse } from "@/domain";
import { AuthService } from "@/api/services/AuthService";
import { DeviceService } from "@/api/services/DeviceService";

const route = useRoute();
const router = useRouter();
const deviceStore = useDeviceStore();
const templateStore = useTemplateStore();
const analyticsStore = useAnalyticsStore();

const socketState = ref<"connecting" | "open" | "closed" | "error">("closed");
const liveEvents = ref<LiveEvent[]>([]);
const commandForms = reactive<Record<string, Record<string, string | number | boolean>>>({});
const commandError = ref("");

let socket: ContactsSocket | null = null;

const consumerId = computed(() => route.params.id as string);
const devices = computed(() => deviceStore.deviceViews(templateStore.templates, templateStore.contractsByTemplate));
const device = computed(() => devices.value.find((item) => item.id === consumerId.value));
const contracts = computed<CommandContract[]>(() => toCommandContracts(device.value?.contracts || []));
const commandContracts = computed(() =>
    contracts.value.filter((contract) => contract.direction === "WRITE" || contract.direction === "READ_WRITE"),
);
const telemetryVariables = computed<VariableResponse[]>(() =>
    device.value ? templateStore.variablesByTemplate[device.value.templateId] || [] : [],
);
const latestTelemetryByName = computed<Record<string, LiveEvent>>(() => {
    const variableNames = new Set(telemetryVariables.value.map((variable) => variable.name));
    return liveEvents.value.reduce<Record<string, LiveEvent>>((result, event) => {
        const name = event.contract_name;
        if (event.type !== "event" || !name || !variableNames.has(name) || result[name]) return result;
        result[name] = event;
        return result;
    }, {});
});

const editingName = ref(false);
const editName = ref("");
const nameInput = ref<HTMLInputElement>();
const showAccessModal = ref(false);
const accessSearch = ref("");
const accessRole = ref("USER");

const grantAccess = async () => {
    if (!accessSearch.value.trim()) return;
    try {
        const users = await AuthService.searchUsers(accessSearch.value);
        if (users.length === 0) {
            commandError.value = "User not found";
            return;
        }
        await DeviceService.grantAccess(consumerId.value, users[0].id, accessRole.value);
        showAccessModal.value = false;
        accessSearch.value = "";
        accessRole.value = "USER";
    } catch (cause) {
        commandError.value = apiErrorMessage(cause);
    }
};

const defaultValue = (type: DomainType): string | number | boolean => {
    if (type === "BOOLEAN") return false;
    if (type === "INTEGER" || type === "FLOAT") return 0;
    if (type === "JSON") return "{}";
    return "";
};

const startEdit = () => {
  editName.value = device.value?.name || "";
  editingName.value = true;
  nextTick(() => nameInput.value?.focus());
};

const saveName = async () => {
  const name = editName.value.trim().slice(0, 100);
  if (!name || !device.value) return;
  await deviceStore.updateConsumerName(consumerId.value, name);
  editingName.value = false;
};

const cancelEdit = () => {
  editingName.value = false;
};

const initForms = () => {
    commandContracts.value.forEach((contract) => {
        if (!commandForms[contract.name]) commandForms[contract.name] = {};
        contract.fields.forEach((field) => {
            if (commandForms[contract.name][field.name] === undefined) {
                commandForms[contract.name][field.name] = defaultValue(field.type);
            }
        });
    });
};

const payloadFor = (contract: CommandContract): unknown => {
    if (contract.fields.length === 0) return {};
    if (contract.fields.length === 1 && contract.fields[0].type === "JSON") {
        return JSON.parse(String(commandForms[contract.name].payload || "{}"));
    }
    return { ...commandForms[contract.name] };
};

const formatTelemetryValue = (event: LiveEvent | undefined): string => {
    if (!event) return "Waiting";
    const payload = event.payload;
    if (payload === null || payload === undefined) return "No value";
    if (typeof payload !== "object") return String(payload);

    const record = payload as Record<string, unknown>;
    if ("value" in record) {
        const unit = typeof record.unit === "string" && record.unit ? ` ${record.unit}` : "";
        return `${String(record.value)}${unit}`;
    }

    return JSON.stringify(payload);
};

const formatTelemetryTime = (event: LiveEvent | undefined): string =>
    event?.ts ? new Date(event.ts).toLocaleTimeString() : "live";

const connectSockets = () => {
    socket?.disconnect();
    liveEvents.value = [];
    if (!device.value) return;

    socket = new ContactsSocket(
        (message) => {
            liveEvents.value = [message, ...liveEvents.value].slice(0, 40);
            if (message.type === "error") {
                commandError.value = message.message || message.code || "Execution error";
            } else if (message.type === "success") {
                commandError.value = "";
            }
        },
        (state) => {
            socketState.value = state;
        },
    );
    const readableVariables = telemetryVariables.value.map((variable) => variable.name);
    socket.connect(
        readableVariables.length > 0
            ? {
                  consumer_id: device.value.id,
                  contracts: readableVariables,
              }
            : undefined,
    );
};

const execute = (contract: CommandContract) => {
    commandError.value = "";
    try {
        socket?.sendCommand({
            request_id: window.crypto.randomUUID(),
            consumer_id: consumerId.value,
            contract_name: contract.name,
            payload: payloadFor(contract),
        });
    } catch (cause) {
        commandError.value = apiErrorMessage(cause);
    }
};

const regenerateToken = async () => {
    try {
        const token = await deviceStore.regenerateToken(consumerId.value);
        window.alert(`Token regenerated successfully!\n\nPlease save this new JWT token:\n\n${token}`);
    } catch (cause) {
        window.alert(`Error regenerating token: ${apiErrorMessage(cause)}`);
    }
};

watch(commandContracts, initForms, { immediate: true });
watch(telemetryVariables, () => connectSockets());
watch(
    device,
    async (current) => {
        if (!current) return;
        await analyticsStore.load({ consumerId: current.id, bucket: "hour", limit: 30 });
        connectSockets();
    },
    { immediate: true },
);

onMounted(async () => {
    const consumer = deviceStore.consumers.find((item) => item.id === consumerId.value);
    if (consumer) await templateStore.ensureTemplateDetail(consumer.templateId);
});

onBeforeUnmount(() => {
    socket?.disconnect();
});
</script>

<template>
    <div v-if="!device" class="panel empty-state">
        <p>Device not found.</p>
        <button class="btn" type="button" @click="router.push('/')">Back to devices</button>
    </div>

    <div v-else class="stack" style="padding-bottom: 2rem">
        <!-- Header -->
        <section class="panel" style="margin-bottom: 1.5rem; background: var(--surface-card)">
            <div class="panel-header" style="border-bottom: 0">
                <div style="display: flex; align-items: center; gap: 1rem">
                    <button
                        class="btn icon-button"
                        type="button"
                        @click="router.push('/')"
                        aria-label="Back to devices"
                    >
                        <i class="pi pi-arrow-left"></i>
                    </button>
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.5rem">
                            <template v-if="editingName">
                                <input
                                    v-model="editName"
                                    class="input"
                                    maxlength="100"
                                    style="font-size: 1.25rem; font-weight: 600; width: 300px"
                                    @keyup.enter="saveName"
                                    @keyup.escape="cancelEdit"
                                    ref="nameInput"
                                />
                                <button class="btn btn-primary" type="button" @click="saveName">
                                    <i class="pi pi-check"></i>
                                </button>
                                <button class="btn" type="button" @click="cancelEdit">
                                    <i class="pi pi-times"></i>
                                </button>
                            </template>
                            <template v-else>
                                <h2 style="margin: 0; font-size: 1.5rem">{{ device.name }}</h2>
                                <button class="icon-button" type="button" @click="startEdit" aria-label="Rename">
                                    <i class="pi pi-pencil"></i>
                                </button>
                            </template>
                        </div>
                        <span class="mono muted">{{ device.templateName }}</span>
                    </div>
                </div>
                <div class="toolbar">
                    <span class="status-badge" :class="socketState">WS: {{ socketState }}</span>
                    <button class="btn" type="button" @click="showAccessModal = true">
                        <i class="pi pi-users"></i> Grant Access
                    </button>
                    <button class="btn" type="button" @click="regenerateToken">
                        <i class="pi pi-refresh"></i> Reset token
                    </button>
                </div>
            </div>

            <!-- Access Modal -->
            <div v-if="showAccessModal" class="modal-backdrop" @click.self="showAccessModal = false">
                <div class="modal access-modal">
                    <div class="access-modal-head">
                        <h3>Grant Access</h3>
                        <button type="button" class="icon-button" @click="showAccessModal = false">
                            <i class="pi pi-times"></i>
                        </button>
                    </div>
                    <div class="access-modal-body">
                        <label class="field">
                            <span>User</span>
                            <input v-model="accessSearch" class="input" placeholder="Search by username or email..." @keyup.enter="grantAccess" />
                        </label>
                        <label class="field">
                            <span>Role</span>
                            <select v-model="accessRole" class="select">
                                <option value="USER">User</option>
                                <option value="VIEWER">Viewer</option>
                                <option value="OWNER">Owner</option>
                            </select>
                        </label>
                        <div class="access-modal-actions">
                            <button type="button" class="btn" @click="showAccessModal = false">Cancel</button>
                            <button type="button" class="btn btn-primary" @click="grantAccess">Grant</button>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- Commands (Contracts) -->
        <section class="panel">
            <div class="panel-header">
                <h2>Commands</h2>
                <span v-if="commandError" class="status-badge error">{{ commandError }}</span>
            </div>
            <div class="panel-body stack">
                <div v-if="commandContracts.length === 0" class="empty-state">No write contracts available for this template.</div>
                <div
                    v-else
                    style="display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1rem"
                >
                    <form
                        v-for="contract in commandContracts"
                        :key="contract.id"
                        class="command-card"
                        @submit.prevent="execute(contract)"
                        style="
                            border: 1px solid var(--surface-border);
                            border-radius: 8px;
                            padding: 1.5rem;
                            background: var(--surface-ground);
                        "
                    >
                        <div style="margin-bottom: 1rem">
                            <h3 style="margin: 0; font-size: 1.1rem">{{ contract.name }}</h3>
                            <p class="muted" style="margin: 0.25rem 0 0 0; font-size: 0.875rem">
                                {{ contract.description || `${contract.input} → ${contract.output}` }}
                            </p>
                        </div>
                        <div class="command-fields stack" style="margin-bottom: 1rem">
                            <label v-for="field in contract.fields" :key="field.name" class="field" style="margin: 0">
                                <span style="font-size: 0.875rem; color: var(--text-muted)"
                                    >{{ field.name }} ({{ field.type }})</span
                                >
                                <input
                                    v-if="field.type === 'BOOLEAN'"
                                    type="checkbox"
                                    :checked="Boolean(commandForms[contract.name][field.name])"
                                    @change="
                                        commandForms[contract.name][field.name] = (
                                            $event.target as HTMLInputElement
                                        ).checked
                                    "
                                />
                                <input
                                    v-else-if="field.type === 'INTEGER' || field.type === 'FLOAT'"
                                    class="input"
                                    type="number"
                                    :value="Number(commandForms[contract.name][field.name])"
                                    @input="
                                        commandForms[contract.name][field.name] = Number(
                                            ($event.target as HTMLInputElement).value,
                                        )
                                    "
                                />
                                <textarea
                                    v-else-if="field.type === 'JSON'"
                                    class="textarea mono"
                                    style="min-height: 80px"
                                    :value="String(commandForms[contract.name][field.name])"
                                    @input="
                                        commandForms[contract.name][field.name] = (
                                            $event.target as HTMLTextAreaElement
                                        ).value
                                    "
                                ></textarea>
                                <input
                                    v-else
                                    class="input"
                                    :value="String(commandForms[contract.name][field.name])"
                                    @input="
                                        commandForms[contract.name][field.name] = (
                                            $event.target as HTMLInputElement
                                        ).value
                                    "
                                />
                            </label>
                        </div>
                        <button
                            class="btn btn-primary"
                            type="submit"
                            :disabled="socketState !== 'open'"
                            style="width: 100%"
                        >
                            <i class="pi pi-play"></i> Execute
                        </button>
                    </form>
                </div>
            </div>
        </section>

        <!-- Variables (Data) from Live Events -->
        <section class="panel">
            <div class="panel-header">
                <h2>Variables Data</h2>
                <span class="muted">Latest values from device</span>
            </div>
            <div class="panel-body">
                <div v-if="telemetryVariables.length === 0" class="empty-state">
                    No variables available for this template.
                </div>
                <div v-else style="display: flex; flex-direction: column; gap: 0.5rem">
                    <div
                        v-for="variable in telemetryVariables"
                        :key="variable.id"
                        style="
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            padding: 0.75rem;
                            background: var(--surface-ground);
                            border-radius: 6px;
                        "
                    >
                        <div style="display: flex; align-items: center; gap: 1rem">
                            <i class="pi pi-bolt" style="color: var(--primary-color)"></i>
                            <div>
                                <strong style="display: block">{{ variable.name }}</strong>
                                <span class="muted" style="font-size: 0.875rem">{{ variable.type }}</span>
                            </div>
                        </div>
                        <div style="text-align: right">
                            <strong class="mono" style="display: block">{{
                                formatTelemetryValue(latestTelemetryByName[variable.name])
                            }}</strong>
                            <span class="mono muted" style="font-size: 0.875rem">{{
                                formatTelemetryTime(latestTelemetryByName[variable.name])
                            }}</span>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- Action History -->
        <section class="panel">
            <div class="panel-header">
                <h2>Action History</h2>
                <span class="muted">Historical actions over the last 3 days</span>
            </div>
            <div class="panel-body">
                <div v-if="analyticsStore.events.length === 0" class="empty-state">No historical events found.</div>
                <div v-else style="display: flex; flex-direction: column; gap: 0.5rem">
                    <div
                        v-for="event in analyticsStore.events"
                        :key="event.event_id"
                        style="
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            padding: 0.75rem;
                            border: 1px solid var(--surface-border);
                            border-radius: 6px;
                        "
                    >
                        <div style="display: flex; align-items: center; gap: 1rem">
                            <i class="pi pi-history" style="color: var(--text-muted)"></i>
                            <div>
                                <strong style="display: block">{{ event.action }}</strong>
                                <span class="muted" style="font-size: 0.875rem"
                                    >Contract: {{ event.contract_name || "N/A" }}</span
                                >
                            </div>
                        </div>
                        <span class="mono muted" style="font-size: 0.875rem">{{
                            new Date(event.occurred_at).toLocaleString()
                        }}</span>
                    </div>
                </div>
            </div>
        </section>
    </div>
</template>

<style scoped>
.access-modal {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 10px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.access-modal-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.25rem;
    border-bottom: 1px solid var(--border);
}

.access-modal-head h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
}

.access-modal-body {
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.access-modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    padding-top: 0.5rem;
}
</style>
