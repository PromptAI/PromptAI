import * as method from '@/utils/request';

export function getList({ projectId, ...rest }) {
  return method.get(`/api/project/component/${projectId}?type=synonym`, { ...rest });
}

export function create({ projectId, ...rest }) {
  return method.post(`/api/project/component/${projectId}`, {
    type: 'synonym',
    ...rest
  });
}

export function update({ projectId, ...rest }) {
  return method.put(`/api/project/component/${projectId}`, {
    projectId,
    ...rest,
    // data: {}
  });
}

export function del({ projectId, id }) {
  return method.del(`/api/project/component/${projectId}`, {
    components: id,
  });
}

export async function listLables(projectId) {
  return method.get(`/api/project/component/synonym/labels?projectId=${projectId}`);
}
