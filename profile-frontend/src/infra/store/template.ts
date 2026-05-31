import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { apiErrorMessage } from '@/api/client';
import { TemplateService } from '@/api/services/TemplateService';
import type {
  ContractResponse,
  CreateContractPayload,
  CreateTemplatePayload,
  CreateVariablePayload,
  DomainType,
  TemplateResponse,
  TemplateState,
  UpdateTemplatePayload,
  VariableResponse,
} from '@/domain';

const DEFAULT_TYPE: DomainType = 'JSON';

export const useTemplateStore = defineStore('template', () => {
  const templates = ref<TemplateResponse[]>([]);
  const variablesByTemplate = ref<Record<string, VariableResponse[]>>({});
  const contractsByTemplate = ref<Record<string, ContractResponse[]>>({});
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const publicTemplates = computed(() => templates.value.filter((template) => template.state === 'PUBLIC'));

  const fetchTemplates = async (params: { state?: TemplateState; search?: string } = {}) => {
    isLoading.value = true;
    error.value = null;
    try {
      templates.value = await TemplateService.getTemplates(params);
    } catch (cause) {
      error.value = apiErrorMessage(cause);
    } finally {
      isLoading.value = false;
    }
  };

  const getTemplateById = (id: string) => templates.value.find((template) => template.id === id);

  const ensureTemplateDetail = async (templateId: string) => {
    if (!templateId) return;
    const [variables, contracts] = await Promise.all([
      TemplateService.getVariables(templateId),
      TemplateService.getContracts(templateId),
    ]);
    variablesByTemplate.value[templateId] = variables;
    contractsByTemplate.value[templateId] = contracts;
  };

  const createTemplate = async (payload: CreateTemplatePayload) => {
    const id = await TemplateService.createTemplate(payload);
    await fetchTemplates();
    return id;
  };

  const updateTemplate = async (id: string, payload: UpdateTemplatePayload) => {
    await TemplateService.updateTemplate(id, payload);
    await fetchTemplates();
  };

  const deleteTemplate = async (id: string) => {
    await TemplateService.deleteTemplate(id);
    await fetchTemplates();
  };

  const createVariable = async (templateId: string, payload: CreateVariablePayload) => {
    await TemplateService.createVariable(templateId, payload);
    await ensureTemplateDetail(templateId);
  };

  const updateVariable = async (templateId: string, variableId: string, payload: CreateVariablePayload) => {
    await TemplateService.updateVariable(variableId, payload);
    await ensureTemplateDetail(templateId);
  };

  const deleteVariable = async (templateId: string, variableId: string) => {
    await TemplateService.deleteVariable(variableId);
    await ensureTemplateDetail(templateId);
  };

  const createContract = async (templateId: string, payload: CreateContractPayload) => {
    await TemplateService.createContract(templateId, payload);
    await ensureTemplateDetail(templateId);
  };

  const updateContract = async (templateId: string, contractId: string, payload: CreateContractPayload) => {
    await TemplateService.updateContract(contractId, payload);
    await ensureTemplateDetail(templateId);
  };

  const deleteContract = async (templateId: string, contractId: string) => {
    await TemplateService.deleteContract(contractId);
    await ensureTemplateDetail(templateId);
  };

  const emptyVariable = (): CreateVariablePayload => ({ name: '', type: DEFAULT_TYPE, description: '' });
  const emptyContract = (): CreateContractPayload => ({ name: '', direction: 'READ_WRITE', input: DEFAULT_TYPE, output: DEFAULT_TYPE, description: '' });

  return {
    templates,
    publicTemplates,
    variablesByTemplate,
    contractsByTemplate,
    isLoading,
    error,
    fetchTemplates,
    getTemplateById,
    ensureTemplateDetail,
    createTemplate,
    updateTemplate,
    deleteTemplate,
    createVariable,
    updateVariable,
    deleteVariable,
    createContract,
    updateContract,
    deleteContract,
    emptyVariable,
    emptyContract,
  };
});
