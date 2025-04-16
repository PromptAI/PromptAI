import { get, put } from '@/utils/request';

export async function updateLLmConfig(params?: any) {
  return put('/api/settings/configurations/llm', params);
}

export async function getLLmConfig() {
  return get('/api/settings/configurations/llm');
}