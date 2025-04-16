import { decode, encode } from 'js-base64';
import QueryString from 'qs';

const PUBLISH_ENGINE = 'rasa';
const CONFIG_KEY = 'config';

interface IParams {
  id: string;
  token: string;
  project: string;
  name: string;
  survey?: boolean;
  scene: 'publish_db' | 'publish_snapshot';
}
export function getConfig(params: IParams, other?: object) {
  params['disableSurvey'] = params.survey;
  params['name'] = params['name'] || 'Prompt AI';
  return QueryString.stringify({
    [CONFIG_KEY]: encode(
      QueryString.stringify(
        { engine: PUBLISH_ENGINE, ...params },
        { indices: true }
      )
    ),
    ...other,
  });
}
export function encodeChatScript(params: IParams) {
  const src = `${window.location.origin}/ava/chatbot.app?${getConfig(params)}`;
  return `<script type="text/javascript" src="${src}"></script>`;
}
export function encodeChatUrl(config: IParams, other: object) {
  return `${window.location.origin}/ava/?${getConfig(config, other)}`;
}
export function decodeChatUrl(url: string) {
  const temp = new URL(url);
  const config = temp.searchParams.get(CONFIG_KEY) || '';
  return QueryString.parse(decode(config));
}
