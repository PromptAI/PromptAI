import { get, post } from '@/utils/request';

export async function packages(type) {
  return get('/api/stripe/packages', type);
}

export async function freeLicense(data) {
  return post('/api/stripe/free/license', data);
}

export async function getStripePublicKey() {
  return get('/api/stripe/config');
}

export async function payment(data) {
  return post('/api/stripe/token/payment', data);
}
