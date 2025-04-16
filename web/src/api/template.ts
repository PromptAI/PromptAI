import { get, post, put, del } from '@/utils/request';

export async function getTemplate(projectId: string) {
  return get('/api/template/' + projectId);
}
