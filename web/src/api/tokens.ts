import { get, post, put, del } from '@/utils/request';

export async function listToken() {
  return get('/api/stripe/token');
}
