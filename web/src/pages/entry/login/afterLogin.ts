import { init } from '@/api/bootsrap';
import store from '@/store';
import Token from '@/utils/token';
import {
  pushEvent,
  LABEL_USER_NAME,
  CATEGORY_LOGIN,
} from '@/utils/baiduAnalytics';
import { Message } from '@arco-design/web-react';
import Singleton, { key_default_account } from '@/utils/singleton';

function redirectPage(response, history) {
  // 登录后跳转到操作前的页面
  const search = window.location.search;
  const params = new URLSearchParams(search);
  const redirect = params.get('redirect');
  if (redirect && redirect !== '') {
    window.location.assign(redirect);
    return;
  }

  const target = '/';
  history.replace(target);
}

export default async function afterLogin(response, history, params, t) {
  Message.success(t['login.success']);
  pushEvent({
    category: CATEGORY_LOGIN,
    optLabel: LABEL_USER_NAME,
    optValue: params.username,
  });

  // 默认账户才 append 这个 key
  const defaultAccount = response.defaultAccount;
  if (defaultAccount && process.env.REACT_APP_AIO !== 'yes') {
    Singleton.set(key_default_account, true);
  } else {
    // 登录了其他系统的账户，移除这个 key
    Singleton.remove(key_default_account);
  }

  // set token
  Token.set(response.token || '', response.tokenExpireAt);
  // set locale login status
  localStorage.setItem('userStatus', 'login');
  // initial user store
  store.dispatch({
    type: 'update-userInfo',
    payload: { userInfo: response.user, userLoading: false },
  });
  redirectPage(response, history);
}
