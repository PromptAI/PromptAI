import { del, get } from '@/utils/request';
import { useRequest } from 'ahooks';
import React, { createContext, useCallback, useContext } from 'react';
import { ComMenu } from './types';
import { Modal } from '@arco-design/web-react';
import { useHistory, useParams } from 'react-router';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

interface DynamicMenu {
  key: string;
  name: string;
  value: string;
}
interface DynamicMenuContextValue {
  loading: boolean;
  dynamicMenus: DynamicMenu[];
  refresh: () => void;
  onDelete: (componentId: string, name: string) => Promise<unknown>;
}
const defaultDynamicMenuContextValue: DynamicMenuContextValue = {
  loading: true,
  dynamicMenus: [],
  refresh: () => void 0,
  onDelete: () => void 0,
};
const DynamicMenuContext = createContext<DynamicMenuContextValue>(
  defaultDynamicMenuContextValue
);

interface DynamicMenuProviderProps {
  menu: ComMenu;
  children: React.ReactNode;
}

function namePath(path: string, data: any) {
  return path.split('.').reduce((p, c) => p[c], data);
}
async function getDynamicMenuConfig(menu: ComMenu): Promise<DynamicMenu[]> {
  if (menu) {
    const { dynamic, path: basePath } = menu;
    const {
      api,
      nameIndex,
      params,
      menuPrefix = '',
      menuSuffix = '',
    } = dynamic;
    return get(api, params).then((data) =>
      data.map((d) => ({
        key: `${menuPrefix}${basePath}/${d.id}/${menuSuffix}`,
        name: namePath(nameIndex, d),
        value: d.id,
      }))
    );
  }
  return Promise.resolve([]);
}
export default function DynamicMenuProvider({
  menu,
  children,
}: DynamicMenuProviderProps) {
  const t = useLocale(i18n);
  const history = useHistory();
  const params = useParams();
  const {
    loading,
    data: dynamicMenus = [],
    refresh,
  } = useRequest(() => getDynamicMenuConfig(menu));
  const onDelete = useCallback(
    (componentId: string, name: string) => {
      return new Promise((resolve, reject) => {
        Modal.confirm({
          title: t['dynamic.delete'],
          content:
            t['dynamic.delete.tip.prefix'] +
            name +
            t['dynamic.delete.tip.suffix'],
          onOk: async () => {
            if (menu?.dynamic) {
              try {
                await del(menu.dynamic.api, { components: componentId });
                const redirect = menu.dynamic.onDeletedRedirectUrl(
                  menu.dynamic,
                  params
                );
                if (redirect) {
                  history.replace(redirect);
                }
                resolve(undefined);
              } catch (e) {
                reject(e);
              }
            }
          },
          footer: (cancel, ok) => [ok, cancel],
        });
      });
    },
    [history, menu?.dynamic, params, t]
  );
  return (
    <DynamicMenuContext.Provider
      value={{ loading, dynamicMenus, refresh, onDelete }}
    >
      {children}
    </DynamicMenuContext.Provider>
  );
}

export function useDynamicMenu() {
  const context = useContext(DynamicMenuContext);
  if (!context) {
    throw new Error('should be in dynamic menu context');
  }
  return context;
}
