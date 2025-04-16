export const CATEGORY_LOGIN = '登录';
export const CATEGORY_REGISTER = '注册';
export const CATEGORY_PROJECT = '项目';
export const CATEGORY_MESSAGE = '消息';

export const ACTION_CLICK = '点击';
export const LABEL_USER_NAME = '用户';

export function pushEvent({
  category,
  action = ACTION_CLICK,
  optLabel,
  optValue,
}) {
  window._hmt.push(['_trackEvent', category, action, optLabel, optValue]);
}
