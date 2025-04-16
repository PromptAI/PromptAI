import { get } from '@/utils/request';

export async function listDownloadItem(projectId: string) {
  return get(`/api/project/pre/download`, { projectId });
}
