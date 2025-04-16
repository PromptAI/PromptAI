import { get, post, del, put } from '@/utils/request';

export async function authLogin(data) {
  return post('/api/auth/login', data);
}
export async function authCode() {
  return get('/api/auth/code');
}
export async function authMe() {
  return get('/api/auth/info');
}

const useMap = {
  email: 'email',
  sms: 'mobile',
};

export async function getSmsCode(username: string, use: 'email' | 'sms') {
  return post('/api/notify/register', {
    type: use,
    [useMap[use]]: username,
  });
}

export async function getLoginSmsCode(username: string, use: 'email' | 'sms') {
  return post('/api/notify/login', {
    type: use,
    [useMap[use]]: username,
  });
}

export async function resetPwdSmsCode(username: string, use: 'email' | 'sms') {
  return post('/api/notify/reset/pwd', {
    type: use,
    [useMap[use]]: username,
  });
}

export async function registerApply(data: any) {
  return post('/api/notify/register', data);
}
export async function register(data: any) {
  return post('/api/auth/quick/signin', data);
}
export async function updatePwd(data: any) {
  return post('/api/settings/users/updatePass', data);
}

export async function loginOut() {
  return del('/api/auth/logout');
}

// export async function updateAvatar(data) {
//   // 上传头像
//   // const formData = new FormData();
//   // formData.append('file', data);
//   return post('/api/settings/users/updateAvatar', );
// }
export async function refreshToken() {
  return get('/api/auth/refresh/token');
}

export async function applying(data: any) {
  return post('/rpc/trial/apply', data, {
    headers: { token: 'ab710887-153e-46e5-9c55-35844f3e5713' },
  });
}
export async function configMe(data) {
  return put('/api/settings/users/config', data);
}
