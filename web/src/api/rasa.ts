import { post, get, put } from '@/utils/request';
import QueryString from 'qs';

export async function trian(data) {
  return post('/api/settings/auto/startTest', data);
}
export async function listen(taskId: string) {
  return get(`/api/settings/auto/tasks/${taskId}`);
}
export async function isHanding() {
  return get('/api/settings/auto/tasks');
}
/// **** new api ****
export async function startTrain(formData: FormData) {
  return post('/api/project/train', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

export async function newStartTrain(object) {
  const form = new FormData();
  for (let i = 0; i < object.componentIds.length; i++) {
    form.append('componentIds', object.componentIds[i]);
  }
  form.append('projectId', object.projectId);
  return post('/api/project/train', form, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

export async function listenTask(taskId: string) {
  return get(`/api/agent/task/${taskId}`);
}

export async function listenRecord(recordId: string) {
  return get(`/api/publish/record/${recordId}`);
}

export async function publishRecordRunning() {
  return get(`/api/publish/record/running`);
}

export async function hasTaskRunning() {
  return get(`/api/agent/task/running`);
}

export async function cancelTask(taskId: string) {
  return put(`/api/agent/task/cancel/${taskId}`);
}
export async function sendMessage(
  params: {
    message: string;
    chatId: any;
    scene: 'publish_db' | 'publish_snapshot' | 'debug';
    content?: string;
  },
  headers?: any
) {
  return post('/chat/api/message', params, { headers });
}
export async function sendNextMessage(
  data: { message: string; chatId: string },
  authtication: Record<string, string>
) {
  return post('/chat/api/message', data, { headers: authtication });
}

export async function createChat(headers?: any, params?: any) {
  return post('/chat/api/chat', params, { headers });
}
export async function getMessageList(params?: any) {
  return get(`/api/chat?${QueryString.stringify(params, { indices: false })}`);
}
export async function fetchMessage(chatId?: any) {
  if (!chatId) return Promise.resolve(null);
  return get(`/api/message/${chatId}`);
}

export const UPLOAD_INPUT_FILE = '/chat/api/message/file';
