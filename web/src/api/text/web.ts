import { del, get, post, put } from '@/utils/request';
import { omit } from 'lodash';

export async function pageWebLibs(projectId: string, params: any) {
  return get(`/api/project/component/${projectId}/kb/url`, params);
}
export async function deleteWebLibs(projectId: string, ids: string[]) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}
export interface IBatchDeleteWebParams {
  projectId: string;
  ids: string[];
}
export async function batchDelelteWeb({
  projectId,
  ids,
}: IBatchDeleteWebParams) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}
export async function createWebLib(projectId: string, data: any) {
  return post(`/api/project/component/${projectId}/kb/url`, data);
}
export async function updateWebLib(projectId: string, id: string, data: any) {
  return put(`/api/project/component/${projectId}`, {
    data,
    id,
    type: 'kb-url',
  });
}
export interface IEnableWebParams {
  projectId: string;
  row: any;
}
export async function enableWeb({ projectId, row }: IEnableWebParams) {
  return put(`/api/project/component/${projectId}`, row);
}

export async function syncWebLib(projectId: string, componentId: string) {
  return post(
    `/api/project/component/${projectId}/kb/url/async/${componentId}`
  );
}

export async function updateContent(projectId: string, data: any) {
  return put(`/api/project/component/${projectId}/kb/url/update`,data);
}
export interface IParseChildLinksParams {
  url: string;
  filter?: string;
  projectId: string;
}
export async function parseChildLinks(
  params: IParseChildLinksParams
): Promise<{ links: string[]; total: number; filtered: number }> {
  return post(
    `/api/project/component/${params.projectId}/kb/url/parse`,
    omit(params, 'projectId')
  );
}
export async function isHandling(
  projectId: string
): Promise<{ handling: boolean; count: number }> {
  return get(`/api/project/component/${projectId}/kb/url/handling`);
}

export async function stopHandling(projectId: string): Promise<void> {
  await del(`/api/project/component/${projectId}/kb/url/handling`);
}
