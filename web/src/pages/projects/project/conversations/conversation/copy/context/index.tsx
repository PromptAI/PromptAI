import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { CopyContextValue } from './type';
import { Message } from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const CopyContext = createContext<CopyContextValue>(null);
const LOCAL_KEY_PREFIX = 'copy_';

export default function CopyContextProvider({ children, flowId }) {
  const [visible, setVisible] = useState(false);
  const [data, setData] = useState(null);
  const t = useLocale(i18n);
  useEffect(() => {
    // init data
    let initialData = null;
    try {
      initialData = JSON.parse(
        window.localStorage.getItem(LOCAL_KEY_PREFIX + flowId)
      );
    } catch (e) {}
    setData(initialData);
  }, [flowId]);
  const submit = useCallback(
    (d) => {
      // update local data
      window.localStorage.setItem(LOCAL_KEY_PREFIX + flowId, JSON.stringify(d));
      setData(d);
      Message.success(t['copy.success']);
      setVisible(true);
    },
    [flowId, t]
  );
  const clear = useCallback(() => {
    window.localStorage.removeItem(LOCAL_KEY_PREFIX + flowId);
    setData(null);
  }, [flowId]);
  const value = useMemo(
    () => ({ data, submit, clear, visible, setVisible }),
    [data, submit, clear, visible]
  );
  return <CopyContext.Provider value={value}>{children}</CopyContext.Provider>;
}

export function useCopy() {
  const context = useContext(CopyContext);
  if (!context) {
    throw new Error('should be in a copy context');
  }
  return context;
}
