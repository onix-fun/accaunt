import { ref } from 'vue';
import { defineStore } from 'pinia';
import { apiErrorMessage } from '@/api/client';
import { AnalyticsService } from '@/api/services/AnalyticsService';
import type {
  ActionAggregateResponse,
  AnalyticsEventResponse,
  AnalyticsQuery,
  TimeSeriesPointResponse,
} from '@/api/services/AnalyticsService';

export const useAnalyticsStore = defineStore('analytics', () => {
  const events = ref<AnalyticsEventResponse[]>([]);
  const actions = ref<ActionAggregateResponse[]>([]);
  const timeseries = ref<TimeSeriesPointResponse[]>([]);
  const nextCursor = ref<string | null>(null);
  const isLoading = ref(false);
  const error = ref<string | null>(null);
  const isHealthy = ref<boolean | null>(null);

  const checkHealth = async () => {
    try {
      isHealthy.value = await AnalyticsService.health();
    } catch {
      isHealthy.value = false;
    }
  };

  const load = async (query: AnalyticsQuery) => {
    if (!query.consumerId) {
      events.value = [];
      actions.value = [];
      timeseries.value = [];
      return;
    }
    isLoading.value = true;
    error.value = null;
    try {
      const [eventPage, actionItems, seriesItems] = await Promise.all([
        AnalyticsService.getEvents({ ...query, limit: query.limit || 50 }),
        AnalyticsService.getActionAggregates(query),
        AnalyticsService.getTimeseries({ ...query, bucket: query.bucket || 'hour' }),
      ]);
      events.value = eventPage.events;
      nextCursor.value = eventPage.next_cursor || null;
      actions.value = actionItems;
      timeseries.value = seriesItems;
    } catch (cause) {
      error.value = apiErrorMessage(cause);
    } finally {
      isLoading.value = false;
    }
  };

  return {
    events,
    actions,
    timeseries,
    nextCursor,
    isLoading,
    error,
    isHealthy,
    checkHealth,
    load,
  };
});
