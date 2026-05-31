import { domainClient } from "@/api/client";
import type {
  ContractResponse,
  CreateContractPayload,
  CreateTemplatePayload,
  CreateVariablePayload,
  IdResponse,
  TemplateResponse,
  TemplateState,
  UpdateTemplatePayload,
  VariableResponse,
} from "@/domain";

export class TemplateService {
  static async getTemplates(params: { state?: TemplateState; search?: string } = {}): Promise<TemplateResponse[]> {
    const response = await domainClient.get<TemplateResponse[]>("/templates", { params });
    return response.data;
  }

  static async getTemplate(id: string): Promise<TemplateResponse> {
    const response = await domainClient.get<TemplateResponse>(`/templates/${id}`);
    return response.data;
  }

  static async createTemplate(payload: CreateTemplatePayload): Promise<string> {
    const response = await domainClient.post<IdResponse>("/templates", payload);
    return response.data.id;
  }

  static async updateTemplate(id: string, payload: UpdateTemplatePayload): Promise<void> {
    await domainClient.patch(`/templates/${id}`, payload);
  }

  static async deleteTemplate(id: string): Promise<void> {
    await domainClient.delete(`/templates/${id}`);
  }

  static async getVariables(templateId: string): Promise<VariableResponse[]> {
    const response = await domainClient.get<VariableResponse[]>(`/templates/${templateId}/variables`);
    return response.data;
  }

  static async createVariable(templateId: string, payload: CreateVariablePayload): Promise<string> {
    const response = await domainClient.post<IdResponse>(`/templates/${templateId}/variables`, payload);
    return response.data.id;
  }

  static async updateVariable(variableId: string, payload: CreateVariablePayload): Promise<void> {
    await domainClient.patch(`/variables/${variableId}`, payload);
  }

  static async deleteVariable(variableId: string): Promise<void> {
    await domainClient.delete(`/variables/${variableId}`);
  }

  static async getContracts(templateId: string): Promise<ContractResponse[]> {
    const response = await domainClient.get<ContractResponse[]>(`/templates/${templateId}/contracts`);
    return response.data;
  }

  static async createContract(templateId: string, payload: CreateContractPayload): Promise<string> {
    const response = await domainClient.post<IdResponse>(`/templates/${templateId}/contracts`, payload);
    return response.data.id;
  }

  static async updateContract(contractId: string, payload: CreateContractPayload): Promise<void> {
    await domainClient.patch(`/contracts/${contractId}`, payload);
  }

  static async deleteContract(contractId: string): Promise<void> {
    await domainClient.delete(`/contracts/${contractId}`);
  }

  static async grantAccess(templateId: string, clientId: string, role: string): Promise<void> {
    await domainClient.put(`/templates/${templateId}/collaborators/${clientId}`, { role });
  }
}
