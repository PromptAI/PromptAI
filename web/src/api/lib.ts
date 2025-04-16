import { get, post } from '@/utils/request';

export async function getPublics() {
  return get('/api/project/public');
}

export async function cloneProject(data) {
  return post('/api/project/public/clone', data);
}
