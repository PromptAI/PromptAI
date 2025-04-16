import { ReactNode } from 'react';
import { RUInstance } from './core';

export interface RedoUndoState {
  redoLoading: boolean;
  redoLength: number;
  undoLoading: boolean;
  undoLength: number;
}

export type StateListener = {
  key: string;
  callback: (state: RedoUndoState) => void;
};
export interface RedoUndoContextValue extends RedoUndoState {
  RU: RUInstance;
}
export interface RedoUndoContextProviderProps {
  children: ReactNode;
  flowId: string;
  RU: RUInstance;
}
type DataItemDependencies = {
  projectId: string;
  children?: any[];
  parent?: any;
  flowId: string;
  other?: any;
  formId?: string;
  breakParentId?: string;
};
export type DataItem = {
  changed: any; // 更改的数据
  dependencies: DataItemDependencies; // 更改数据需要的依赖
  type: string; // 更改操作类型
};

type DataItemHandler = (item: DataItem) => Promise<string> | Promise<void>;

export type DataItemHandlerMap = Record<
  string,
  { redo: DataItemHandler; undo: DataItemHandler }
>;

export type EmitHandler = (state: RedoUndoState, eventKey: string) => void;
