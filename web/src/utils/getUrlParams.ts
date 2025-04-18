// 仅用于线上预览，实际使用中可以将此逻辑删除
import qs from 'query-string';
import { isSSR } from './is';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type ParamsType = Record<string, any>;

export default function getUrlParams(): [ParamsType, string] {
  const params = qs.parseUrl(!isSSR ? window.location.href : '').query;
  const returnParams: ParamsType = {};
  Object.keys(params).forEach((p) => {
    if (params[p] === 'true') {
      returnParams[p] = true;
    }
    if (params[p] === 'false') {
      returnParams[p] = false;
    }
  });
  return [returnParams, window.location.pathname];
}
