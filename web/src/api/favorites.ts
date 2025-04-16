import { del, get, put } from '@/utils/request';

export async function getFavoritesList(type, params?) {
  return get(`/api/project/component/collection`, { type, ...params });
}
export async function unFavorites(ids: string[]) {
  return del(`/api/project/component/collection`, { ids: ids.join(',') });
}
export async function doFavorite(data) {
  return put('/api/project/component/collection', data);
}
export async function doFavoritePaste(
  components: any[],
  projectId,
  rootComponentId
) {
  return put(
    `/api/project/component/${projectId}/${rootComponentId}/paste`,
    components.map((c) =>
      c.type === 'user' ? { ...c, afterRhetorical: null } : c
    )
  );
}

export async function getFavoritesFaqList(params?) {
  return getFavoritesList('faq-root', params);
}

export async function getFavoritesFlowList(params?) {
  return getFavoritesList('conversation', params);
}
