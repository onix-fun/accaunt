import { domainClient } from "@/api/client";
import type {
  ConsumerResponse,
  CreateConsumerPayload,
  CreateConsumerResponse,
  RegenerateTokenResponse,
} from "@/domain";

export class DeviceService {
  static async getConsumers(params: { templateId?: string } = {}): Promise<ConsumerResponse[]> {
    const response = await domainClient.get<ConsumerResponse[]>("/consumers", { params });
    return response.data;
  }

  static async createConsumer(payload: CreateConsumerPayload): Promise<CreateConsumerResponse> {
    const response = await domainClient.post<CreateConsumerResponse>("/consumers", payload);
    return response.data;
  }

  static async updateName(consumerId: string, displayName: string): Promise<void> {
    await domainClient.put(`/consumers/${consumerId}/name`, { displayName });
  }

  static async regenerateToken(consumerId: string): Promise<RegenerateTokenResponse> {
    const response = await domainClient.post<RegenerateTokenResponse>(`/consumers/${consumerId}/token/regenerate`);
    return response.data;
  }

  static async grantAccess(consumerId: string, clientId: string, role: string): Promise<void> {
    await domainClient.put(`/consumers/${consumerId}/collaborators/${clientId}`, { role });
  }
}
