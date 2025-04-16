import { put } from '@/utils/request';

export async function moveNode(data) {
  return put(`/api/project/component/resort`, data);
}
