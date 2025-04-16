import { Message, Modal } from '@arco-design/web-react';
import axios from 'axios';
import Token from './token';
import local from '../locale';
import React from 'react';
import { isEmpty } from 'lodash';
import SystemRedirect from '@/utils/systemRedirect';

const backendLocaleKeyMap = {
  'en-US': 'en-US,en;q=0.9',
  'zh-CN': 'zh-CN,zh;q=0.9',
};
const transformBackendLocale = () => {
  let lang = window.localStorage.getItem('lang');
  if (isEmpty(lang)) {
    lang = 'zh-CN';
  }
  return backendLocaleKeyMap[lang] || 'zh-CN';
};

const instance = axios.create({
  timeout: 6 * 1e4,
  baseURL: '/',
});

const UnAuthUrls = ['/api/auth/login', '/api/auth/code'];
let UnAuthenticationCount = 0;
instance.interceptors.request.use(
  (options) => {
    const { headers, url } = options;
    let contentType = 'application/json;charset=UTF-8';
    if (headers['Content-Type'] || headers['content-type']) {
      contentType = headers['Content-Type'] || headers['content-type'];
    }
    // headers['X-account-name'] = 'core'; // 创建 public project user
    // headers['X-account-name'] = 'flow2';

    headers['Content-Type'] = contentType;
    if (!UnAuthUrls.some((u) => u === url)) {
      // auth token
      headers.Authorization = Token.get();
    }
    // local token
    headers['Accept-Language'] = transformBackendLocale();

    return options;
  },
  (error) => {
    // config error
    return Promise.reject(error);
  }
);
const jsonContentTypes = [
  'application/json;charset=UTF-8',
  'application/json',
  'application/json;',
];
instance.interceptors.response.use(
  (response) => {
    const t = local[window.localStorage.getItem('lang')] || local['zh-CN'];
    const { status, data, headers } = response;
    if (199 < status && status < 300) {
      const isJson = jsonContentTypes.includes(
        headers['content-type'] || headers['Content-Type']
      );
      UnAuthenticationCount = 0;
      if (isJson) {
        return Promise.resolve(data);
      }
      return Promise.resolve(response);
    }
    Message.error(t['request.unknown-error']);
    return Promise.reject(data);
  },
  async (error) => {
    const t = local[window.localStorage.getItem('lang')] || local['zh-CN'];
    const {
      status,
      config: { url },
    } = error.response || {};
    let data = error.response.data;
    if (data instanceof Blob) {
      const text = await data.text();
      try {
        data = JSON.parse(text);
      } catch (e) {
        // no handle
      }
    }
    if (status === 400) {
      Message.warning(data?.message || t['request.400']);
      return Promise.reject(data);
    }
    // Token 过期后请求这里
    if (status === 401) {
      if (UnAuthenticationCount === 0) {
        UnAuthenticationCount++;
        Modal.confirm({
          title: t['request.401-403.title'],
          content: t['request.401-403.content'],
          okButtonProps: { status: 'danger' },
          onOk: () => {
            return new Promise((resolve) => {
              setTimeout(() => {
                resolve(null);
                window.localStorage.setItem('userStatus', 'logout');
                // 带着当前 url跳转
                SystemRedirect.redirect2Login();
              }, 1000);
            });
          },
          footer: (cancel, ok) =>
            React.createElement(React.Fragment, null, [ok, cancel]),
        });
      }
      return Promise.reject(data);
    }
    if (status === 404 && url === '/api/sysadmin/account') {
      window.location.href = '/404';
      return Promise.reject(data);
    }

    Message.error(data?.message || t['request.unknown-error']);
    return Promise.reject(data);
  }
);
export default instance;
export function get(url: string, params?: any, config?: any): Promise<any> {
  return instance.get(url, params ? { params, ...config } : config);
}

export function post(url: string, data?: any, config?: any): Promise<any> {
  return instance.post(url, data, config);
}
export function put(url: string, data?: any, config?: any): Promise<any> {
  return instance.put(url, data, config);
}
export function del(url: string, data?: any, config?: any): Promise<any> {
  return instance.delete(url, {
    ...config,
    params: data,
  });
}
