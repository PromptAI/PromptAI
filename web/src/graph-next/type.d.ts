import { GraphTreeValue } from '@/core-next/types';

export interface IDName {
  id: string;
  name: string;
}
export interface Relations {
  relations: string[];
  linkedFrom?: IDName;
  type: string;
}
interface ValidInfo {
  validatorError: {
    errorCode: number;
    errorMessage: string;
  };
}

export interface GraphNode extends GraphTreeValue, Relations, ValidInfo {}

export interface FieldData {
  slotName: string;
  slotId: string;
  description?: string;
}

export interface GraphField extends GraphNode {
  data: FieldData;
  type: 'field';
}

export interface ConversationData {
  name: string;
  description: string;
  hidden?: boolean;
  canEditorHidden?: boolean;
}

export interface GraphConversation extends GraphNode {
  data: ConversationData;
  type: 'conversation';
}

export interface Mark {
  start: number;
  end: number;
  name?: string;
  entityId?: string;
  targetId?: string;
}

export interface Example {
  key?: string;
  text: string;
  marks: Mark[];
}

export interface IntentMapping {
  id: string;
  slotId: string;
  slotName: string;
  slotDisplay: string;
  type: 'from_entity' | 'from_intent' | 'from_text';
  value?: string;
  enable: boolean;
  multiInput?: boolean;
}

export interface IntentNextData {
  name: string;
  examples: Example[];
  mappingsEnable: boolean;
  mappings: IntentMapping[];
  setSlots?: SetSlot[];
  display: 'user_click' | 'user_input';
}

export interface GraphIntentNext extends GraphNode {
  data: IntentNextData;
  type: 'user';
  afterRhetorical?: string;
}

export interface ComponentRelation {
  usedByComponentRoots: {
    rootComponentId: string;
    componentId: string;
    rootComponentType: 'faq' | 'flow';
  }[];
}

export interface GlobalIntent {
  id: string;
  data: IntentNextData;
  type: 'user-global';
  componentRelation: ComponentRelation;
}

export interface IntentData {
  examples: string[];
}

export interface GraphIntent extends GraphNode {
  data: IntentData;
  type: 'user';
}

/// 文本响应
export interface BotResponseBaseContent {
  text: string;
}

// Action 响应
export interface BotResponseActionContent extends BotResponseBaseContent {
  code: string;
}

/// 图片响应
export interface Image {
  id: string;
  url: string;
}

export interface BotResponseImageContent extends BotResponseBaseContent {
  image: Image[];
}

/// 按钮响应
export interface ButtonOption {
  id: string;
  label: string;
  value: string;
}

export interface BotResponseButtonContent extends BotResponseBaseContent {
  options: ButtonOption[];
}

/// webhook响应
export interface KV {
  key: string;
  value: string;
}

export interface ResponseHandle {
  parse: KV[];
  text: string;
  error_msg: string;
}

// export interface BotResponseWebhookContent extends BotResponseBaseContent {
//   id: string;
//   url: string;
//   headers?: KV[];
//   params?: KV[];
//   request_type: 'get' | 'get_with_url_encode' | 'post_form' | 'post_json';
//   response_type: 'not' | 'direct' | 'custom';
//   response_handle?: ResponseHandle;
//   description?: string;
// }

export interface BotResponseWebhookContent extends BotResponseBaseContent {
  id: string;
  url: string;
  request_header_type: 'custom' | 'none';
  headers?: KV[];
  request_type: 'get' | 'post' | 'put' | 'delete';
  response_type:
    | 'ignore_response'
    | 'ignore_http_code'
    | 'origin_response'
    | 'custom';
  response_handle?: ResponseHandle;
  description?: string;
  request_body_type:
    | 'multipart/form-data'
    | 'none'
    | 'application/json'
    | 'text/plain';
  request_body: string;
}

export interface BotResponse<T extends BotResponseBaseContent> {
  id: string;
  type: string;
  content: T;
  delay?: number;
}

export interface SetSlot {
  slotId: string;
  value: any;
}
export interface BotData<T extends BotResponseBaseContent> {
  setSlots?: SetSlot[];
  responses: BotResponse<T>[];
}

export type BotDataType =
  | BotResponseBaseContent
  | BotResponseImageContent
  | BotResponseButtonContent
  | BotResponseWebhookContent
  | BotResponseActionContent;

export interface GraphBot extends GraphNode {
  data: BotData<BotDataType>;
  type: 'bot' | 'rhetorical';
  componentRelation: ComponentRelation;
}

export interface From {
  entity?: string;
  intent: string[];
  notIntent: string[];
  value?: string;
}

export interface RhetoricalData {
  slot: string;
  rhetorical: string;
  from_entity: From[];
  from_intent: From[];
  from_trigger_intent: From[];
  from_text: From[];
}

export interface GraphRhetorical extends GraphNode {
  data: RhetoricalData;
  type: 'rhetorical';
}

export interface GraphRhetoricalNext extends GraphBot {
  data: BotData<BotResponseBaseContent>;
  type: 'rhetorical';
}

export interface GotoData {
  name: string;
  linkId: string;
  description: string;
}

export interface GraphGoto extends GraphNode {
  data: GotoData;
  type: 'goto';
}

export interface BreakData {
  formId: string;
  name: string;
  conditionId?: string;
  color: string;
}

export interface ConditionData {
  name: string;
  color: string;
}

export interface GraphBreak extends GraphNode {
  data: BreakData;
  type: 'break';
}

export interface GraphCondition extends GraphNode {
  data: ConditionData;
  type: 'condition';
}

export interface GraphReturn extends GraphNode {
  data: any;
  type: 'return';
}

export interface OptionData {
  examples: string[];
  description: string;
  setSlots?: SetSlot[];
}

export interface GraphOption extends GraphNode {
  data: OptionData;
  type: 'option';
}

export interface FaqData {
  name: string;
}

export interface GraphFaq extends GraphNode {
  data: FaqData;
  type: 'faq-root';
}

export interface GraphEntity {
  id: string;
  name: string;
  description?: string;
  type: 'entity';
}

export interface FromIntent {
  entity?: string;
  intent: string[];
  notIntent: string[];
  value?: string;
}

export interface FormData {
  name: string;
  description?: string;
}

export interface GraphForm extends GraphNode {
  data: FormData;
  type: 'form';
}

export interface GraphSlots extends GraphNode {
  data: {
    examples: Example[];
  };
  type: 'slots';
}

export interface GraphConfirm extends GraphNode {
  data: {
    //
  };
  type: 'confirm';
}

export interface GraphInterrupt extends GraphNode {
  data: {
    //
  };
  type: 'interrupt';
}

type Entity = IDName;
type SlotMappingIntent = IDName;
type SlotMappingScope = IDName;

export interface SlotMapping {
  id: string;
  type: 'from_entity' | 'from_intent' | 'from_text';
  entity: Entity;
  intent: SlotMappingIntent;
  notIntent: SlotMappingIntent;
  scope: SlotMappingScope;
}

export interface Slot {
  id: string;
  name: string;
  display: string;
  type: 'any';
  influenceConversation: false;
  mappings: SlotMapping[];
  blnInternal?: boolean;
  data?: any[];
  slotType?: 'string' | 'integer' | 'boolean' | 'array';
  enumEnable?: boolean;
  enum?: string[];
  defaultValueEnable?: boolean;
  defaultValueType?: 'set' | 'localStore' | 'sessionStore' | 'custom';
  defaultValue?: string;
}

/// Rasa json struct
export type RasaProjectData = {
  name: string;
  locale: 'zh' | 'en';
};
export type RasaIntent = {
  intent: string;
  examples: string[];
};
export type RasaButton = {
  title: string;
  payload: string;
};
export type RasaWebhookHandle = {
  parse: Record<string, string>;
  text: string;
  error_msg: string;
};
// export type RasaResponse = {
//   utter_name: string;
//   text: string;
//   custom?: Record<string, any>;
//   buttons?: RasaButton[];
//   image?: string;
//   // webhook response properties
//   label?: string;
//   url?: string;
//   headers?: Record<string, string>;
//   params?: Record<string, string>;
//   request_type?: string;
//   response_type?: string;
//   response_handle?: RasaWebhookHandle;
// };
export type RasaResponse = {
  utter_name: string;
  text: string;
  custom?: any;
  buttons?: RasaButton[];
  image?: string;
  // webhook response properties
  label?: string;
  url?: string;
  headers?: Record<string, string>;
  request_type?: string;
  response_type?: string;
  response_handle?: RasaWebhookHandle;
  request_body?: string;
  request_header_type?: 'custom' | 'none';
  request_body_type?:
    | 'multipart/form-data'
    | 'none'
    | 'application/json'
    | 'text/plain';
};
export type RasaSlotMapping = {
  type: 'from_entity' | 'from_text' | 'from_intent';
  entity?: string;
  value?: string;
  intent: string[];
  notIntent: [];
  scope: string[];
};
export type RasaSlot = {
  slotName: string;
  type: 'any';
  influenceConversation: false;
  mappings: RasaSlotMapping[];
};

export interface RasaNode {
  id: string;
  label: string;
  parent: string;
  kind:
    | 'user'
    | 'bot'
    | 'form'
    | 'slots'
    | 'confirm'
    | 'interrupt'
    | 'faq'
    | 'break'
    | 'condition';
  data: any;
  children?: RasaNode[];
}

export type RasaUserNodeMappingData = {
  type: 'from_entity' | 'from_text' | 'from_intent';
  entity?: string[];
  value?: string;
};
export type RasaUserNodeData = {
  intent: RasaIntent;
  mapping?: RasaUserNodeMappingData;
};

export interface RasaUserNode extends RasaNode {
  data: RasaUserNodeData;
  kind: 'user';
}

export type RasaBotNodeData = {
  responses: RasaResponse[];
};

export interface RasaBotNode extends RasaNode {
  data: RasaBotNodeData;
  kind: 'bot';
}

export type RasaForm = {
  name: string;
  required_slots: string[];
};
export type RasaFormNodeData = {
  name: string;
};

export interface RasaFormNode extends RasaNode {
  data: RasaFormNodeData;
  kind: 'form';
}

export type RasaFaqData = {
  model: string;
};

export interface RasaFaq extends RasaNode {
  data: RasaFaqData;
  kind: 'faq';
}

export type RasaBreakData = {
  conditionId: string;
};

export interface RasaBreak extends RasaNode {
  data: RasaBreakData;
  kind: 'break';
}

export type RasaProject = {
  id: string;
  label: string;
  data: RasaProjectData;
  actions: string[];
  customs: Record<string, string>;
  stories: string;

  intents: RasaIntent[];
  responses: Record<string, RasaResponse>;

  entities: string[];
  slots: RasaSlot[];
  forms: RasaForm[];
  children: RasaNode[];
};
