import * as base64 from 'js-base64';
import QueryString from 'qs';

function transformStringToBoolean(object) {
  return Object.entries(object).reduce((p, c) => {
    const [key, value] = c;
    let val = value;
    if (value === 'true') {
      val = true;
    }
    if (value === 'false') {
      val = false;
    }
    return { ...p, [key]: val };
  }, {});
}
export const qs = (() => {
  // { config: '...', origin: '...' } => { id, name, project, ..., origin }
  const res = new URLSearchParams(window.location.search);
  const settings = QueryString.parse(base64.decode(res.get('config') || ''));
  if (res.has('origin')) {
    settings['origin'] = res.get('origin');
  }
  return transformStringToBoolean(settings);
})();
export const theme = (() => {
  const res = new URLSearchParams(window.location.search);
  return res.get('theme') || 'default';
})();
export const injectVars = (() => {
  const res = new URLSearchParams(window.location.search);
  let slots = res.get('slots');
  let variables = res.get('variables');
  try {
    slots = slots ? JSON.parse(slots) : [];
    variables = variables ? JSON.parse(variables) : [];
    console.log({ slots, variables });
    return { slots, variables };
  } catch (error) {
    console.log(error);
    return { slots: [], variables: [] };
  }
})();
const creator = window.top;

export const isPreview = (() => {
  const res = new URLSearchParams(window.location.search);
  return res.has('preview');
})();
export const isOnlyChatbot = (() => {
  const res = new URLSearchParams(window.location.search);
  return res.has('only-chatbot');
})();

export const hideBotName = (() => {
  const res = new URLSearchParams(window.location.search);
  return res.has('hide-bot-name');
})();

export const autoOpen = (() => {
  const res = new URLSearchParams(window.location.search);
  return res.has('auto-open');
})();

export const locale = (() => {
  return new URLSearchParams(window.location.search).get('locale') || 'zh';
})();
export const DEFAULT_LANG = locale;
export const isIntegrated = creator && window.top !== window && !isPreview && !isOnlyChatbot;

const invoker = isIntegrated
  ? (value) => creator.postMessage(String(value), qs['origin'])
  : () => void 0;

export default invoker;
