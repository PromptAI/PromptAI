import { del, get, put } from '@/utils/request';
import QueryString from 'qs';

export async function moveInTrash(projectId, rootComponentId, componentId, formId: string) {
  return put(
    `/api/project/component/${projectId}/${rootComponentId}/trash/${componentId}?${QueryString.stringify({ formId }, { indices: false })}`
  );
}
export async function getTrash(projectId, rootComponentId) {
  return get(`/api/project/component/${projectId}/${rootComponentId}/trash`);
}
export async function moveOutTrash(
  projectId,
  rootComponentId,
  componentId,
  parentId,
  formId: string,
) {
  return put(
    `/api/project/component/${projectId}/${rootComponentId}/trash/putback/${componentId}?${QueryString.stringify(
      { parentId, formId },
      { indices: false }
    )}`
  );
}
export async function clearTrash(
  projectId,
  rootComponentId,
  components: string[]
) {
  return del(
    `/api/project/component/${projectId}/${rootComponentId}/trash/empty?${QueryString.stringify(
      { components },
      { indices: false }
    )}`
  );
}
