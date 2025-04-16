import { ComponentRelation, IDName, IntentNextData } from '@/graph-next/type';
import { del, get, post, put } from '@/utils/request';
import QueryString from 'qs';

export type Types =
  | 'faq-root'
  | 'user'
  | 'option'
  | 'bot'
  | 'conversation'
  | 'webhook'
  | 'entity'
  | 'form'
  | 'slots'
  | 'confirm'
  | 'interrupt'
  | 'rhetorical'
  | 'goto'
  | 'return'
  | 'break'
  | 'condition'
  | 'any' // slot
  | 'field'
  | 'action'
  | 'form-gpt'
  | 'slots-gpt'
  | 'slot-gpt'
  | 'functions-gpt'
  | 'function-gpt'
  | 'abort-gpt'
  | 'completed-gpt'
  | 'gpt'
  | 'agent';

async function listComponents(projectId: string, type: Types, params?: any) {
  return get(`/api/project/component/${projectId}`, { type, ...params });
}
export async function listAction(projectId: string) {
  return listComponents(projectId, 'action');
}
export async function batchCreateComponent(
  projectId: string,
  flowId: string,
  components: any[]
) {
  return post(
    `/api/project/component/${projectId}/${flowId}/batch`,
    components
  );
}
export async function createComponent(
  projectId: string,
  type: Types,
  other: any
) {
  return post(`/api/project/component/${projectId}`, {
    type,
    ...other,
  });
}

export async function infoComponent(projectId: string, id: string) {
  return get(`/api/project/component/${projectId}/${id}`);
}

export async function updateComponent(
  projectId: string,
  type: Types,
  id: string,
  other: any
) {
  return put(`/api/project/component/${projectId}`, {
    id,
    type,
    ...other,
  });
}
export interface UpdateCompArgs {
  projectId: string;
  id: string;
  [k: string]: string;
}
export async function updateComp(args: UpdateCompArgs) {
  const { projectId, ...rest } = args;
  return put(`/api/project/component/${projectId}`, rest);
}

export async function deleteComponent(projectId: string, ids: string[]) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}

export interface IBatchDeleteComponentParams {
  projectId: string;
  ids: string[];
}
export async function batchDeleteComponent({
  projectId,
  ids,
}: IBatchDeleteComponentParams) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}

/// ************************用户问答(FAQ)**************************

export async function listFaqs(projectId: string) {
  return listComponents(projectId, 'faq-root');
}

export async function createFaq(
  projectId: string,
  data,
  relations: string[],
  welcome: string
) {
  return createComponent(projectId, 'faq-root', {
    data,
    relations,
    welcome,
  });
}

export async function updateFaq(
  projectId: string,
  fId: string,
  data,
  relations: string[]
) {
  return updateComponent(projectId, 'faq-root', fId, {
    data,
    relations,
  });
}

export async function infoFaq(projectId: string, fId: string) {
  return infoComponent(projectId, fId);
}

export async function deleteFaq(projectId: string, fIds: string[]) {
  return deleteComponent(projectId, fIds);
}

/// ************************Webhook**************************

export async function listWebhooks(projectId: string) {
  return listComponents(projectId, 'webhook');
}

export async function createWebhook(projectId: string, data) {
  return createComponent(projectId, 'webhook', data);
}

export async function updateWebhook(projectId: string, wId: string, data) {
  return updateComponent(projectId, 'webhook', wId, data);
}

export async function infoWebhook(projectId: string, wId: string) {
  return infoComponent(projectId, wId);
}

export async function deleteWebhook(projectId: string, ids: string[]) {
  return deleteComponent(projectId, ids);
}

/// ************************User(Intent)**************************

export async function listIntents(projectId: string, params?: any) {
  return listComponents(projectId, 'user', params);
}

export async function createNextIntent(
  projectId: string,
  parentId: string,
  relations: string[],
  data: IntentNextData,
  afterRhetorical?: string
) {
  return createComponent(projectId, 'user', {
    parentId,
    relations,
    data,
    afterRhetorical,
  });
}

export async function updateNextIntent(
  projectId: string,
  id: string,
  parentId: string,
  relations: string[],
  data: Partial<IntentNextData>,
  linkedFrom?: IDName,
  afterRhetorical?: string
) {
  return updateComponent(projectId, 'user', id, {
    parentId,
    relations,
    data,
    linkedFrom,
    afterRhetorical,
  });
}

export async function createIntent(
  projectId: string,
  parentId: string,
  data,
  relations: string[]
) {
  return createComponent(projectId, 'user', {
    parentId,
    data,
    relations,
  });
}

export async function updateIntent(
  projectId: string,
  uId: string,
  data,
  relations: string[],
  parentId?: string,
  afterRhetorical?: string,
  linkedFrom?: string
) {
  return updateComponent(projectId, 'user', uId, {
    parentId,
    data,
    relations,
    afterRhetorical,
    linkedFrom,
  });
}

export async function deleteIntent(projectId: string, uIds: string[]) {
  return deleteComponent(projectId, uIds);
}

/// ************************Option(Intent alias type)**************************

export async function createIntentOption(
  projectId: string,
  parentId: string,
  data,
  relations: string[],
  keyword = ''
) {
  return createComponent(projectId, 'user', {
    parentId,
    data,
    relations,
    keyword,
  });
}

export async function updateIntentOption(
  projectId: string,
  oId: string,
  data,
  relations: string[],
  parentId: string
) {
  return updateComponent(projectId, 'user', oId, {
    parentId,
    data,
    relations,
  });
}

export async function deleteIntentOption(projectId: string, oIds: string[]) {
  return deleteComponent(projectId, oIds);
}

/// ************************Bot**************************

export async function listBots(projectId: string) {
  return listComponents(projectId, 'bot');
}

export async function createBot(
  projectId: string,
  parentId: string,
  data: any,
  relations: string[],
  componentRelation: ComponentRelation
) {
  return createComponent(projectId, 'bot', {
    parentId,
    data,
    relations,
    componentRelation,
  });
}

export async function updateBot(
  projectId: string,
  bId: string,
  data,
  relations: string[],
  parentId: string,
  componentRelation: ComponentRelation,
  linkedFrom?: any
) {
  return updateComponent(projectId, 'bot', bId, {
    parentId,
    data,
    relations,
    componentRelation,
    linkedFrom,
  });
}

export async function deleteBot(projectId: string, bIds: string[]) {
  return deleteComponent(projectId, bIds);
}
/// ************************Conversation(Flow)**************************

export function getAgents({ projectId, ...rest }) {
  return get(`/api/project/component/agent/${projectId}`, { ...rest });
}


/// ************************Conversation(Flow)**************************

export async function listConversations(projectId: string) {
  return listComponents(projectId, 'conversation');
}

export async function listAgents(projectId: string) {
  return listComponents(projectId, 'agent');
}

export async function createConversation(
  projectId: string,
  data,
  relations: string[],
  welcome: string
) {
  return createComponent(projectId, 'conversation', {
    data,
    relations,
    welcome,
  });
}

export async function updateConversation(
  projectId: string,
  cId: string,
  data,
  relations: string[],
  welcome: string
) {
  return updateComponent(projectId, 'conversation', cId, {
    data,
    relations,
    welcome,
  });
}

export async function infoConversation(projectId: string, cId: string) {
  return infoComponent(projectId, cId);
}

export async function deleteConversation(projectId: string, cIds: string[]) {
  return deleteComponent(projectId, cIds);
}

/// ************************Conversation(Flow)**************************

export async function listEntities(projectId: string) {
  return listComponents(projectId, 'entity');
}

export async function createEntity(projectId: string, data) {
  return createComponent(projectId, 'entity', data);
}

export async function updateEntity(projectId: string, eId: string, data) {
  return updateComponent(projectId, 'entity', eId, data);
}

export async function infoEntity(projectId: string, eId: string) {
  return infoComponent(projectId, eId);
}

export async function deleteEntity(projectId: string, eIds: string[]) {
  return deleteComponent(projectId, eIds);
}

/// ************************Form**************************

export async function listForms(projectId: string) {
  return listComponents(projectId, 'form');
}

export async function createForm(
  projectId: string,
  parentId: string,
  data: any,
  relations: string[]
) {
  return createComponent(projectId, 'form', {
    data,
    relations,
    parentId,
  });
}

export async function updateForm(
  projectId: string,
  parentId: string,
  fmId: string,
  data: any,
  relations: string[]
) {
  return updateComponent(projectId, 'form', fmId, {
    data,
    relations,
    parentId,
  });
}

export async function infoForm(projectId: string, fmId: string) {
  return infoComponent(projectId, fmId);
}

export async function deleteForm(projectId: string, fmIds: string[]) {
  return deleteComponent(projectId, fmIds);
}

/// ************************Break**************************
export async function createBreak(
  projectId: string,
  parentId: string,
  data: any,
  relations: string[]
) {
  return createComponent(projectId, 'break', {
    data,
    relations,
    parentId,
  });
}

export async function updateBreak(
  id: string,
  projectId: string,
  parentId: string,
  data: any,
  relations: string[]
) {
  return updateComponent(projectId, 'break', id, {
    data,
    relations,
    parentId,
  });
}

export async function updateGoto(
  id: string,
  projectId: string,
  parentId: string,
  data: any,
  relations: string[]
) {
  return updateComponent(projectId, 'goto', id, {
    data,
    relations,
    parentId,
  });
}

export async function updateCondition(
  id: string,
  projectId: string,
  parentId: string,
  data: any,
  relations: string[]
) {
  return updateComponent(projectId, 'condition', id, {
    data,
    relations,
    parentId,
  });
}

export async function deleteBreak() {
  return Promise.resolve();
}

/// ************************Rhetorical**************************
export async function createRhetorical(
  projectId: string,
  parentId: string,
  data,
  relations: string[]
) {
  return createComponent(projectId, 'rhetorical', {
    data,
    parentId,
    relations,
  });
}

export async function updateRhetorical(
  projectId: string,
  parentId: string,
  rId: string,
  data,
  relations: string[]
) {
  return updateComponent(projectId, 'rhetorical', rId, {
    data,
    relations,
    parentId,
  });
}

export async function deleteRhetorical(projectId: string, rIds: string[]) {
  return deleteComponent(projectId, rIds);
}

export async function getFromOptions(
  projectId: string
): Promise<{ entities: any[]; intents: any[] }> {
  return Promise.all([
    listEntities(projectId),
    listIntents(projectId, { haveKeyword: true, haveLinked: false }),
  ]).then(([ens, ins]) => ({ entities: ens, intents: ins }));
}

export async function getStories(projectId: string) {
  return get(`/api/project/component/${projectId}`, { type: 'stories' });
}

export async function updateStories(projectId: string, data: any) {
  return put(`/api/project/component/${projectId}`, {
    ...data,
    type: 'stories',
  });
}

export async function createGoto(
  projectId: string,
  parentId: string,
  linkId: string,
  name: string,
  description: string,
  relations: string[]
) {
  return createComponent(projectId, 'goto', {
    parentId,
    data: { name, linkId, description },
    relations,
  });
}

export async function createReturn(
  projectId: string,
  parentId: string,
  relations: string[]
) {
  return createComponent(projectId, 'return', {
    parentId,
    data: {},
    relations,
  });
}

export async function createSlotComponent(projectId: string, data: any) {
  return createComponent(projectId, 'any', data);
}

export async function listSlotComponent(projectId: string) {
  return listComponents(projectId, 'any');
}
export interface ListCompSlotsArgs {
  projectId: string;
  [k: string]: any;
}
export async function listCompSlots(args: ListCompSlotsArgs) {
  const { projectId, ...rest } = args;
  return listComponents(projectId, 'any', rest);
}

export async function updateSlotComponent(
  projectId: string,
  oldId: string,
  data: any
) {
  return updateComponent(projectId, 'any', oldId, data);
}
export async function updateCompSlot(args: UpdateCompArgs) {
  return updateComp({ ...args, type: 'any' });
}

export async function listShareIntents(projectId: string) {
  return listComponents(projectId, 'user', { hasName: true });
}

export async function createField(
  projectId: string,
  parentId: string,
  data: any,
  relations: string[]
) {
  return createComponent(projectId, 'field', {
    data,
    parentId,
    relations,
  });
}

export async function updateField(
  projectId: string,
  oldId: string,
  parentId: string,
  data: any,
  relations: string[]
) {
  return updateComponent(projectId, 'field', oldId, {
    data,
    parentId,
    relations,
  });
}

export async function downloadRasaFile(params: any) {
  return get(
    `/api/project/download?${QueryString.stringify(params, {
      indices: false,
    })}`,
    null,
    {
      responseType: 'blob',
    }
  );
}
