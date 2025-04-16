import { get, post } from '@/utils/request';

export async function leatestSurvey() {
  return get('/api/sysadmin/survey');
}
export async function callSurvey(data: { to: string }) {
  return post('/api/sysadmin/survey', data);
}
