import type { CommandContract, ContractResponse } from '@/domain/template/types';
import { contractFields } from '@/domain/template/types';

export function toCommandContracts(contracts: ContractResponse[]): CommandContract[] {
  return contracts.map((contract) => ({
    id: contract.id,
    name: contract.name,
    direction: contract.direction || 'READ_WRITE',
    input: contract.input,
    output: contract.output,
    description: contract.description,
    fields: contractFields(contract),
  }));
}
