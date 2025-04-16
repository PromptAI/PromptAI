import { GraphNode } from '@/graph-next/type';

export type State = {
  flowId: string | null;
  projectId: string | null;

  loading: boolean;
  visible: boolean;
  selection: GraphNode | null;
  nodes: GraphNode[];
  originNodes: Omit<GraphNode, 'children' | 'parent'>[];
};
export type Action = {
  initial: (initialState: Partial<State>) => void;
  refresh: () => Promise<void>;
  setSelection: (node: GraphNode | null) => void;
  refreshNodes: () => void;
};
type Store = State & Action;
export default Store;
