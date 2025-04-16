import { del, get, post, put } from '@/utils/request';

export async function pagePdfLibs(projectId: string, params: any) {
  return get(`/api/project/component/${projectId}/kb/file`, params);
}
export async function deletePdfLibs(projectId: string, ids: string[]) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}
export interface IBatchDeleteFileParams {
  projectId: string;
  ids: string[];
}
export async function batchDelelteFile({
  projectId,
  ids,
}: IBatchDeleteFileParams) {
  return del(`/api/project/component/${projectId}`, {
    components: ids.join(','),
  });
}

const FILE_TYPE = '3';
export async function uploadPdfLib(
  projectId: string,
  file: File,
  other?: { id?: string; description?: string }
) {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', FILE_TYPE);
    post('/api/blobs/upload', formData).then(
      ({ originalFileName, id: fileId }) => {
        post(`/api/project/component/${projectId}`, {
          id: other.id,
          type: 'kb-file',
          projectId,
          data: { fileId, originalFileName, description: other.description },
        }).then(resolve, reject);
      },
      reject
    );
  });
}
export interface IEnableFileParams {
  projectId: string;
  row: any;
}
export async function enableFile({ projectId, row }: IEnableFileParams) {
  return put(`/api/project/component/${projectId}`, row);
}
