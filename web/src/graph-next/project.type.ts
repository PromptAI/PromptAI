export interface ProjectIntent {
  intent: string;
  examples: string[];
}
interface ProjectResponseButtonItem {
  title: string;
  payload: string;
}
interface ProjectResponseHandle {
  parse: Record<string, string>;
  text: string;
  error_msg: string;
}
export interface ProjectResponse {
  utter_name: string;
  text: string;
  buttons?: ProjectResponseButtonItem[];
  image?: string;
  label?: string;
  url?: string;
  headers?: Record<string, string>;
  params?: Record<string, string>;
  request_type?: string;
  response_type?: string;
  response_handle?: ProjectResponseHandle;
}
export interface ProjectSlot {
  slotName: string;
  type: string;
  autofill: boolean;
  influenceConversation: boolean;
  initialValue: null | string;
  value: null | string;
}
export interface ProjectFromIntent {
  type: 'from_entity' | 'from_text' | 'from_intent' | 'from_trigger_intent';
  entity?: string;
  intent: string[];
  notIntent: string[];
  value?: string;
}
export interface ProjectForm {
  name: string;
  required_slots: Record<string, ProjectFromIntent[]>;
}

export default interface Project {
  id: string;
  label: string;
  data: {
    name: string;
    locale: string;
    createTime: number;
    updateTime: number;
  };
  intents: ProjectIntent[];
  responses: Record<string, ProjectResponse>;
  actions: string[];
  customs: Record<string, string>;
  entities: string[];
  slots: ProjectSlot[];
  forms: ProjectForm[];
  stories: string;
  children: any[];
}
