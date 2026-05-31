import type { ContractResponse, TemplateMetadata, TemplateResponse } from "@/domain/template/types";

export type ConsumerRole = "OWNER" | "USER" | "VIEWER";

export interface ConsumerResponse {
  id: string;
  templateId: string;
  displayName: string;
  createdAt: string;
}

export interface CreateConsumerPayload {
  templateId: string;
  displayName?: string;
}

export interface CreateConsumerResponse {
  id: string;
  token: string;
}

export interface RegenerateTokenResponse {
  token: string;
}

export interface DeviceView {
  id: string;
  name: string;
  templateId: string;
  templateName: string;
  metadata: TemplateMetadata;
  createdAt: string;
  state: "online" | "idle" | "warning" | "offline";
  lastEvent?: string;
  role: ConsumerRole;
  contracts: ContractResponse[];
  template?: TemplateResponse;
}

export interface LiveEvent {
  type: "event" | "success" | "error" | "subscription" | "pong";
  request_id?: string;
  consumer_id?: string;
  contract_name?: string;
  status?: string;
  code?: string;
  message?: string;
  payload?: unknown;
  ts?: string;
  accepted?: string[];
  denied?: string[];
}
