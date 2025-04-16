import { get } from '@/utils/request';
import { keyBy } from 'lodash';

export async function dashboardTotal(params: any) {
  return get('/api/dashboard/chat', params);
}

export async function dashboardBranch(params: any) {
  return get('/api/dashboard/chat/branch', params);
}

export async function dashboardEvaluate(params: any) {
  return get('/api/dashboard/message/evaluate', params);
}

export async function dashboardMessages(params: any) {
  return get('/api/dashboard/message', params);
}

export async function dashboardFallbacks(params: any) {
  const response = await get('/api/dashboard/message/fallback', params);
  return response.map((r) => ({
    count: r.count,
    name: `${r.name}(${r.type})`,
  }));
}

export async function dashboardMessagesFallback(params: any) {
  const fallbacks = await get('/api/dashboard/message/fallback', {
    ...params,
    type: 'day',
  });
  const fmap = keyBy(fallbacks, 'day');
  const messages = await dashboardMessages(params);

  return messages.map(({ day, count }) => ({
    day,
    fcount: fmap[day].count,
    count: `${Number(count) - Number(fmap[day].count)}`,
  }));
}
