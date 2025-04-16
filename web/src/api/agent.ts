import { del, get, post, put } from '@/utils/request';
import QueryString from 'qs';

export async function getAgentTip() {
  return get('/api/agent/available');
}
export async function getAgentPage(params: any) {
  return get(`/api/agent?${QueryString.stringify(params, { indices: false })}`);
}
export async function addAgent(data: any) {
  return post('/api/agent', data);
}

export async function deleteAgent(agentIds: any[]) {
  return del(`/api/agent/${agentIds[0]}`);
}

export async function getAgentCommand(data: any) {
  return get(`/api/agent/install/${data.agentId}`);
}
export async function defaultAgent(agentId: any) {
  return put(`/api/agent/default/${agentId}`);
}
