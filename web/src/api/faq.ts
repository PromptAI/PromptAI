import { get, post, put } from '@/utils/request';
import QueryString from 'qs';

export function getFaqList({ projectId, ...rest }) {
  return get(`/api/project/component/faq/${projectId}`, { ...rest });
}

export function createFaq({ projectId, ...rest }) {
  return post(`/api/project/component/faq/${projectId}`, { ...rest });
}

export function updateFaq({ projectId, ...rest }) {
  return put(`/api/project/component/faq/${projectId}`, { ...rest });
}

export async function listFaqLables(projectId) {
  return get(`/api/project/component/faq/labels?projectId=${projectId}`);
}

export async function downloadFaqs(projectId, componentIds?: string[]) {
  return get(
    `/api/project/component/faq/download/${projectId}?${QueryString.stringify(
      { componentIds },
      { indices: false }
    )}`,
    null,
    {
      responseType: 'blob',
    }
  );
}
