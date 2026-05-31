export type TemplateState = 'DRAFT' | 'PUBLIC' | 'ARCHIVED';
export type ContractDirection = 'READ' | 'WRITE' | 'READ_WRITE';
export type DomainType =
  | 'STATUS'
  | 'TEXT'
  | 'INTEGER'
  | 'FLOAT'
  | 'BOOLEAN'
  | 'JSON'
  | 'UUID'
  | 'DATE'
  | 'TIMESTAMP';

export interface TemplateResponse {
  id: string;
  name: string;
  state: TemplateState;
  info: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateMetadata {
  displayName?: string;
  location?: string;
  icon?: string;
  criticality?: 'low' | 'medium' | 'high';
  tags?: string[];
}

export interface VariableResponse {
  id: string;
  templateId: string;
  name: string;
  type: DomainType;
  description?: string | null;
}

export interface ContractResponse {
  id: string;
  templateId: string;
  name: string;
  direction: ContractDirection;
  input: DomainType;
  output: DomainType;
  description?: string | null;
}

export interface CreateTemplatePayload {
  name: string;
  info?: string;
}

export interface UpdateTemplatePayload {
  name?: string;
  state?: TemplateState;
  info?: string;
}

export interface CreateVariablePayload {
  name: string;
  type: DomainType;
  description?: string | null;
}

export interface CreateContractPayload {
  name: string;
  direction: ContractDirection;
  input: DomainType;
  output: DomainType;
  description?: string | null;
}

export interface IdResponse {
  id: string;
}

export interface CommandField {
  name: string;
  type: DomainType;
}

export interface CommandContract {
  id: string;
  name: string;
  direction: ContractDirection;
  input: DomainType;
  output: DomainType;
  description?: string | null;
  fields: CommandField[];
}

export function parseTemplateInfo(info: string | null | undefined): TemplateMetadata {
  if (!info) return {};
  try {
    const parsed = JSON.parse(info) as TemplateMetadata;
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

export function templateDisplayName(template: TemplateResponse | undefined): string {
  if (!template) return 'Unknown template';
  return parseTemplateInfo(template.info).displayName || template.name;
}

export function contractFields(contract: ContractResponse): CommandField[] {
  if (contract.input === 'JSON') {
    return [{ name: 'payload', type: 'JSON' }];
  }
  if (contract.input === 'STATUS') {
    return [];
  }
  return [{ name: 'value', type: contract.input }];
}
