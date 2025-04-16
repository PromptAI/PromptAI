import { getFavoritesList } from '@/api/favorites';
import { useRequest } from 'ahooks';
import { keyBy } from 'lodash';
import React, { useContext, useEffect, useMemo, useState } from 'react';
import { titleHandlerMapping } from '../../trash/context';
import { FavoritesContextValue, FavoriteStoreItem } from './types';

const FavoritesContext = React.createContext<FavoritesContextValue>(undefined);

const buildItems = (data?: any[]) => {
  if (!data) return [];
  return data.map<FavoriteStoreItem>((d) => {
    const {
      properties: { data: nodes },
    } = d;
    const idMap = keyBy(nodes, 'id');
    const top = nodes.find((n) => !idMap[n.parentId]);
    return {
      key: top.id,
      title: titleHandlerMapping[top?.type]?.(top?.data) || '-',
      type: top.type,
      data: {
        items: nodes,
        breakpoint: top,
      },
    };
  });
};
export default function FavoritesContextProvider({ children, type }) {
  const [visible, setVisible] = useState(false);
  const { loading, data, refresh } = useRequest<
    { data: FavoriteStoreItem[] },
    []
  >(() => getFavoritesList(type), {
    manual: true,
    refreshDeps: [type],
  });
  useEffect(() => {
    visible && refresh();
  }, [visible, refresh]);
  const items = useMemo(() => buildItems(data?.data), [data]);

  return (
    <FavoritesContext.Provider
      value={useMemo(
        () => ({
          loading,
          items,
          refreshFavorites: refresh,
          type,
          visible,
          setVisible,
        }),
        [items, loading, refresh, type, visible]
      )}
    >
      {children}
    </FavoritesContext.Provider>
  );
}

export function useFavorites() {
  const context = useContext(FavoritesContext);
  if (!context) {
    throw new Error('should be in a favorites context');
  }
  return context;
}
