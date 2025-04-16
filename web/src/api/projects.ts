import { del, get, post, put } from '@/utils/request';

export async function listProjects(params?: any) {
  return get('/api/project', params);
}
export async function createProject(data) {
  return post('/api/project', data);
}
export async function updateProject(data) {
  return put('/api/project', data);
}
export async function infoProject(projectId: string) {
  return get(`/api/project/${projectId}`);
}
export async function deleteProject(id: string) {
  return del(`/api/project/${id}`);
}

export async function importYamlProject(data) {
  return post('/api/project/mica/yaml', data);
}
export async function importZipProject(data) {
  return post('/api/project/mica/zip', data);
}

export async function sysadmin(params) {
  return get('/api/sysadmin/account', params);
}

export async function addAccount(data) {
  return post('/api/sysadmin/account', data);
}

export async function removeAccount(id: string) {
  return del(`/api/sysadmin/account/${id}`);
}

export async function suspendedAccount(data) {
  return post('api/sysadmin/account/status', data);
}

export async function editAccount(data) {
  const { accountExtId, ...rest } = data;
  return put(`/api/sysadmin/account/${accountExtId}`, { ...rest });
}

export async function registCodes(params) {
  return get('/api/sysadmin/registry/code', params);
}

export async function createRegisteCode(data = {}) {
  return post('/api/sysadmin/registry/code', data);
}
export async function cancelRegisteCode(id: string) {
  return put(`/api/sysadmin/registry/code/cancel/${id}`);
}

export async function addNote(data) {
  const { accountExtId, ...rest } = data;
  return post(`/api/sysadmin/account/${accountExtId}/notes`, { ...rest });
}

export async function startPublish(pId, componentIds) {
  const form = new FormData();
  for (let i = 0; i < componentIds.length; i++) {
    form.append('componentIds', componentIds[i]);
  }
  form.append('projectId', pId);
  return post('/api/project/publish', form);
}

export async function stopPublish(projectId) {
  return del(`/api/project/stop/${projectId}`);
}

export async function publish(id) {
  return get(`/api/published/project/${id}`);
}

export async function componentInfo(id: string) {
  return get(`/api/project/component/single/${id}`);
}

export async function updateConfigurations(configurations: any) {
  return put('/api/sysadmin/configuration', configurations);
}

export async function trialApplyList(params) {
  return get('/api/sysadmin/trial/apply', params);
}

export async function trialApplyPass(data) {
  return put('/api/sysadmin/trial/apply', data);
}

export async function stripeList() {
  return get('/api/sysadmin/stripe/package');
}

export async function updateStripe(data) {
  return put('/api/sysadmin/stripe/package', data);
}

export async function addStripe(data) {
  return post('/api/sysadmin/stripe/package', data);
}

export async function deleteStripe(id) {
  return del('/api/sysadmin/stripe/package/' + id);
}
