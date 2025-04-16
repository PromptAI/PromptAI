import { GraphNode } from '@/graph-next/type';
import React, { SetStateAction } from 'react';

export type State = {
  projectId: string;

  loading: boolean;
  selection: GraphNode | null;
  nodes: GraphNode[];
};
export type Action = {
  initial: (initialState: Partial<State>) => void;
  refresh: () => Promise<void>;
  setSelection: (node: GraphNode | null) => void;
  refreshNodes: () => void;
};
type Store = State & Action;
export default Store;

export interface SelectionProps<T extends GraphNode> {
  selection: T;
  onChangeSelection?: React.Dispatch<SetStateAction<GraphNode>>;
  projectId: string;
  onChange?: React.Dispatch<SetStateAction<GraphNode[]>>;
  onChangeEditSelection?: React.Dispatch<SetStateAction<GraphNode>>;
  graph: any[];
}
