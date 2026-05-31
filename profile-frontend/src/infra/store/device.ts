import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { apiErrorMessage } from "@/api/client";
import { DeviceService } from "@/api/services/DeviceService";
import { AnalyticsService } from "@/api/services/AnalyticsService";
import { parseTemplateInfo, templateDisplayName } from "@/domain";
import type { ConsumerResponse, DeviceView, TemplateResponse, ContractResponse } from "@/domain";

export const useDeviceStore = defineStore("device", () => {
  const consumers = ref<ConsumerResponse[]>([]);
  const lastEventsByConsumer = ref<Record<string, string>>({});
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const fetchConsumers = async () => {
    isLoading.value = true;
    error.value = null;
    try {
      consumers.value = await DeviceService.getConsumers();
    } catch (cause) {
      error.value = apiErrorMessage(cause);
    } finally {
      isLoading.value = false;
    }
  };

  const createConsumer = async (templateId: string, displayName?: string) => {
    const result = await DeviceService.createConsumer({ templateId, displayName });
    await fetchConsumers();
    return result;
  };

  const updateConsumerName = async (consumerId: string, displayName: string) => {
    await DeviceService.updateName(consumerId, displayName);
    const consumer = consumers.value.find((c) => c.id === consumerId);
    if (consumer) consumer.displayName = displayName;
  };

  const regenerateToken = async (consumerId: string) => {
    const result = await DeviceService.regenerateToken(consumerId);
    return result.token;
  };

  const hydrateLastEvents = async () => {
    await Promise.all(
      consumers.value.slice(0, 12).map(async (consumer) => {
        try {
          const response = await AnalyticsService.getEvents({ consumerId: consumer.id, limit: 1 });
          const [latest] = response.events;
          if (latest) lastEventsByConsumer.value[consumer.id] = latest.action;
        } catch {
          lastEventsByConsumer.value[consumer.id] = "";
        }
      }),
    );
  };

  const deviceViews = computed(
    () =>
      (templates: TemplateResponse[], contractsByTemplate: Record<string, ContractResponse[]>): DeviceView[] =>
        consumers.value.map((consumer) => {
          const template = templates.find((item) => item.id === consumer.templateId);
          const metadata = parseTemplateInfo(template?.info);
          const contracts = contractsByTemplate[consumer.templateId] || [];
          const lastEvent = lastEventsByConsumer.value[consumer.id];
          return {
            id: consumer.id,
            name: consumer.displayName || metadata.displayName || `${templateDisplayName(template)} ${consumer.id.slice(0, 8)}`,
            templateId: consumer.templateId,
            templateName: templateDisplayName(template),
            metadata,
            createdAt: consumer.createdAt,
            state: lastEvent ? "online" : "idle",
            lastEvent,
            role: "OWNER",
            contracts,
            template,
          };
        }),
  );

  return {
    consumers,
    lastEventsByConsumer,
    isLoading,
    error,
    deviceViews,
    fetchConsumers,
    createConsumer,
    updateConsumerName,
    regenerateToken,
    hydrateLastEvents,
  };
});
