import { get, post, put, del } from '@/utils/request';

export async function pageLicenses(params: any) {
  return get('/api/manage/license', params);
}
export async function createLicense(data?: any) {
  return post('/api/manage/license', data);
}

export async function deleteLicense(id: string) {
  return del(`/api/manage/license/${id}`);
}

export async function getLicense() {
  return get('/api/license');
}

export async function setLicense(data: any) {
  return put('/api/license', data);
}

export async function isLicenseOk() {
  return get('/api/license/status');
}
