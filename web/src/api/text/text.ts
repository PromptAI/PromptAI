import { del, get, post, put } from '@/utils/request';

export async function pageTextLibs(projectId: string, params: any) {
  return get(`/api/project/component/${projectId}/kb/text`, params);
}
export async function deleteTextLibs(projectId: string, ids: string[]) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}

export interface IBatchDeleteTextLibParams {
  projectId: string;
  ids: string[];
}
export async function batchDelelteTextLib({
  projectId,
  ids,
}: IBatchDeleteTextLibParams) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}

export async function createTextLib(projectId: string, data: any) {
  return post(`/api/project/component/${projectId}`, { data, type: 'kb-text' });
}
export async function updateTextLib(projectId: string, id: string, data: any) {
  return put(`/api/project/component/${projectId}`, {
    data,
    id,
    type: 'kb-text',
  });
}
export interface IEnableTextLibParams {
  projectId: string;
  row: any;
}
export async function enableTextLib({ projectId, row }: IEnableTextLibParams) {
  return put(`/api/project/component/${projectId}`, row);
}

export async function updateLibDesc({
  desc,
  projectId,
  componentId,
}: {
  desc: string;
  projectId: string;
  componentId: string;
}) {
  return put(`/api/project/component/${projectId}/kb/${componentId}/desc`, {
    description: desc,
  });
}
