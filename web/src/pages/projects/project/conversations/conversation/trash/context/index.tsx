import { getTrash } from '@/api/trash';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import { useRequest } from 'ahooks';
import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { TrashContextValue, TrashStoreItem } from './type';
import ru, { CHANGE_MANAGER_EVENT } from '../../features/ru';

const TrashContext = createContext<TrashContextValue>(undefined);
export const titleHandlerMapping = {
  break: (data) => data.name || 'Break',
  condition: (data) => data.name || 'Condition',
  confirm: () => 'Confirm',
  field: (data) => data?.slotDisplay || '-',
  flow: (data) => data.name || '-',
  form: (data) => data.name || '-',
  user: (data) => data.examples?.[0]?.text || '-',
  interrupt: () => 'Interrupts',
  option: (data) => data.examples?.[0] || '-',
  bot: (data) => data.responses?.[0]?.content.text || '-',
  rhetorical: (data) => data.responses?.[0]?.content.text || '-',
  slots: () => 'Slots',
};
const buildItems = (data): TrashStoreItem[] => {
  const getNodeTitle = (top) => {
    const { type, data } = top;
    const handler = titleHandlerMapping[type];
    return handler ? handler(data) : '-';
  };
  if (!data) return [];
  return data.map(({ top, data: items }) => ({
    key: top.id,
    title: getNodeTitle(top),
    type: top.type,
    data: {
      items,
      breakpoint: top,
    },
  }));
};
export function ContextProvider({ children }) {
  const { flowId, projectId } = useUrlParams();
  const [visible, setVisible] = useState(false);
  const { loading, data, refresh } = useRequest<TrashStoreItem[], []>(
    () => getTrash(projectId, flowId),
    {
      manual: true,
      refreshDeps: [flowId, projectId],
    }
  );
  useEffect(() => {
    visible && refresh();
  }, [refresh, visible]);
  const items = useMemo(() => buildItems(data), [data]);
  useEffect(() => {
    ru.eventer.addListener({
      key: CHANGE_MANAGER_EVENT,
      callback: (manager) => {
        manager.eventer.addListener({
          key: 'reload_trash',
          callback: () => refresh(),
        });
      },
    });
  }, [refresh]);
  return (
    <TrashContext.Provider
      value={useMemo(
        () => ({ loading, items, refreshTrash: refresh, visible, setVisible }),
        [items, loading, refresh, visible]
      )}
    >
      {children}
    </TrashContext.Provider>
  );
}
export function useTrash() {
  const context = useContext(TrashContext);
  if (!context) {
    throw new Error('should be in a trash context');
  }
  return context;
}
