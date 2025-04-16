import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { Spin } from '@arco-design/web-react';
import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { initialRedoUndoState } from './core';
import { RedoUndoContextProviderProps, RedoUndoContextValue } from './types';

const RedoUndoContext = createContext<RedoUndoContextValue>(undefined);

export function RedoUndoContextProvider({
  children,
  flowId,
  RU,
}: RedoUndoContextProviderProps) {
  const t = useLocale(i18n);
  const [state, setState] = useState(initialRedoUndoState);
  useEffect(() => {
    RU.eventer.addListener({
      key: 'change_state',
      callback: (st) => {
        setState(st);
      },
    });
  }, [flowId, RU]);
  const value = useMemo(() => ({ ...state, RU }), [state, RU]);
  return (
    <RedoUndoContext.Provider value={value}>
      <Spin
        loading={state.redoLoading || state.undoLoading}
        tip={t['ru.context.loading']}
        className="w-full"
      >
        {children}
      </Spin>
    </RedoUndoContext.Provider>
  );
}
export function useRedoUndo() {
  const context = useContext(RedoUndoContext);
  if (!context) {
    throw new Error('should be in a redo-undo context');
  }
  return context;
}
