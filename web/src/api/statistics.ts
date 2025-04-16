import { get } from '@/utils/request';

export interface IParams {
  projectId: string;
  startTime: string | number;
  endTime: string | number;
}
export async function summary({ projectId, ...parmas }: IParams) {
  return get(`/api/statistics/${projectId}/summary`, parmas);
}
export interface IParamsWithPage extends IParams {
  page: number;
  size: number;
}
export async function faq({ projectId, ...params }: IParamsWithPage) {
  return get(`/api/statistics/${projectId}/faq`, params);
}
export interface IParamsWithFlowId extends IParams {
  flowId: string;
}
export async function flowExport({
  projectId,
  flowId,
  ...params
}: IParamsWithFlowId) {
  return get(`/api/statistics/${projectId}/flow/${flowId}/export`, params, {
    responseType: 'blob',
  });
}
export async function flow({ projectId, ...params }: IParams) {
  return get(`/api/statistics/${projectId}/flow`, params);
}

export interface IFlowInfoParams extends IParamsWithPage {
  flowId: string;
}
export async function flowInfo({
  projectId,
  flowId,
  ...params
}: IFlowInfoParams) {
  return get(`/api/statistics/${projectId}/flow/${flowId}`, params);
}

export async function fallback({ projectId, ...params }: IParamsWithPage) {
  return get(`/api/statistics/${projectId}/fallback`, params);
}
export async function knowledgeBase({ projectId, ...params }: IParamsWithPage) {
  return get(`/api/statistics/${projectId}/knowledge/base`, params);
}
