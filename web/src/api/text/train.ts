import { post } from '@/utils/request';

export async function trainLib(projectId: string, componentId: string) {
  return post(`/api/project/component/${projectId}/kb/train`, { componentId });
}
export interface IBatchTrainWebParams {
  projectId: string;
  ids: string[];
}
export async function batchTrainWeb({ projectId, ids }: IBatchTrainWebParams) {
  return post(`/api/project/component/${projectId}/kb/url/train`, { ids });
}
