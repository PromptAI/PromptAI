import { post, get, put, del } from '@/utils/request';
import QueryString from 'qs';

// project Id
export async function pageCycleTasks(params: any) {
  const data = await get(
    `/api/schedule/task?${QueryString.stringify(params, {
      indices: false,
    })}`
  );
  return { contents: data, totalElements: data.length };
}
export async function infoCycleTask(id) {
  return get(`/api/schedule/task/${id}`);
}
export async function createCycleTask(data) {
  return post(`/api/schedule/task/train`, data);
}
export async function updateCycleTask(data) {
  return put(`/api/schedule/task/train`, data);
}
export async function deleteCycleTask(id) {
  return del(`/api/schedule/task/train/${id}`);
}
