import { GlobalIntent } from '@/graph-next/type';
import { del, get, post, put } from '@/utils/request';
import qs from 'qs';

export async function createGlobalIntent(
  projectId: string,
  intent: GlobalIntent
) {
  return post(`/api/project/component/${projectId}`, intent);
}

export async function listOfGlobalIntent(projectId: string) {
  return get(`/api/project/component/${projectId}`, { type: 'user-global' });
}

export async function updateGlobalIntent(
  projectId: string,
  intent: GlobalIntent
) {
  return put(`/api/project/component/${projectId}`, intent);
}

export async function delGlobalIntent(projectId: string, components: string[]) {
  return del(
    `/api/project/component/${projectId}?${qs.stringify(
      { components },
      {
        indices: false,
      }
    )}`
  );
}

export async function createGlobalBot(projectId: string, bot: any) {
  return post(`/api/project/component/${projectId}`, bot);
}

export async function listOfGlobalBot(projectId: string) {
  return get(`/api/project/component/${projectId}`, { type: 'bot-global' });
}

export async function updateGlobalBot(projectId: string, bot: any) {
  return put(`/api/project/component/${projectId}`, bot);
}

export async function delGlobalBot(projectId: string, components: string[]) {
  return del(
    `/api/project/component/${projectId}?${qs.stringify(
      { components },
      {
        indices: false,
      }
    )}`
  );
}
