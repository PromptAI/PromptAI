import { get, put } from '@/utils/request';
import { cloneDeep } from 'lodash';

export async function infoLLM() {
  return get('/api/settings/configurations/llm');
}

export async function updateLLM(data: any) {
  const clone = cloneDeep(data);
  clone.system = {};
  return put('/api/settings/configurations/llm', clone);
}
