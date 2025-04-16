import { encrypt } from '@/utils/encrypt';
import { post } from '@/utils/request';

type RestPwdParams = {
  oldPass?: string;
  newPass: string;
  code: string;
  type: 'email' | 'pwd';
  confirm?: string;
};
export const restPwd = (params: RestPwdParams) => {
  const target: RestPwdParams = {
    ...params,
    oldPass: params.oldPass ? encrypt(params.oldPass) : undefined,
    newPass: encrypt(params.newPass),
  };
  delete target['confirm'];
  return post('/api/settings/users/updatePass', target);
};
type RestPwdForgotParams = {
  email: string;
  code: string;
  newPass: string;
};
export const resetForgotPwd = (params: RestPwdForgotParams) => {
  return post('/api/auth/reset/pass', params);
};

export const getForgotPwdCodeByEmail = async (username: string) => {
  return post('/api/notify/forget/pwd', { type: 'email', email: username });
};
export const checkCode = async (data: { token: string; code: string }) => {
  return post('/api/notify/pre/check', data);
};
