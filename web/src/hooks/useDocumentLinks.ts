import { GlobalContext } from '@/context';
import { useContext, useMemo } from 'react';

type DocumentLinks = {
  website: string;
  welcome: string;
  installAgent: string;
  runBot: string;
  example: string;
  botAction: string;
  projectSettings: string;
  dynamicSlot: string;
  knowledgeBase: string;
  slotDictionary: string;
  reqeustSettings: string;
  responseSettings: string;
};
type DocumentRecords = {
  'zh-CN': DocumentLinks;
  'en-US': DocumentLinks;
};
const records: DocumentRecords = {
  'zh-CN': {
    website: 'https://www.promptai.cn/',
    welcome: 'https://doc.promptai.cn/docs/about/',
    installAgent:
      'https://doc.promptai.cn/docs/common_questions/install_questions/#%E5%A6%82%E4%BD%95%E5%88%A0%E9%99%A4%E4%BB%A3%E7%90%86',
    runBot: 'https://doc.promptai.cn/docs/run_bots/',
    example: 'https://doc.promptai.cn/docs/example/',
    botAction: 'https://doc.promptai.cn/docs/tutorial/bot_action/',
    projectSettings:
      'https://doc.promptai.cn/docs/tutorial/release/project_settings/',
    dynamicSlot:
      'https://doc.promptai.cn/docs/advance_control/define_slot_code/',
    knowledgeBase: 'https://doc.promptai.cn/docs/knowledge_base/',
    slotDictionary: 'https://doc.promptai.cn/docs/tutorial/slot_dictionary/',
    reqeustSettings:
      'https://doc.promptai.cn/docs/webhook/01-webhook/#%E8%AF%B7%E6%B1%82%E8%AE%BE%E7%BD%AE',
    responseSettings:
      'https://doc.promptai.cn/docs/webhook/01-webhook/#%E5%93%8D%E5%BA%94%E8%AE%BE%E7%BD%AE',
  },
  'en-US': {
    website: 'https://www.promptai.us/',
    welcome: 'https://doc.promptai.us/docs/about/',
    installAgent:
      'https://doc.promptai.us/docs/common_questions/install_questions/#how-to-uninstall-the-agent-locally',
    runBot: 'https://doc.promptai.us/docs/run_bots/',
    example: 'https://doc.promptai.us/docs/example/',
    botAction: 'https://doc.promptai.us/docs/tutorial/bot_action/',
    projectSettings:
      'https://doc.promptai.us/docs/tutorial/release/project_settings/',
    dynamicSlot:
      'https://doc.promptai.us/docs/advance_control/define_slot_code/',
    knowledgeBase: 'https://doc.promptai.us/docs/knowledge_base/',
    slotDictionary: 'https://doc.promptai.us/docs/tutorial/slot_dictionary/',
    reqeustSettings:
      'https://doc.promptai.us/docs/webhook/01-webhook/#request-settings',
    responseSettings:
      'https://doc.promptai.us/docs/webhook/01-webhook/#response-settings',
  },
};
export default function useDocumentLinks() {
  const { lang } = useContext(GlobalContext);
  return useMemo<DocumentLinks>(() => records[lang], [lang]);
}
