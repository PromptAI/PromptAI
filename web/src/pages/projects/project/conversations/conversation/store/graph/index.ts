import { createWithEqualityFn } from 'zustand/traditional';
import Store from './types';
import { shallow } from 'zustand/shallow';
import { isEmpty, keyBy } from 'lodash';
import { infoConversation } from '@/api/components';
import { buildTree, flattenTree, treeForEach } from '@/utils/tree';

const store = createWithEqualityFn<Store>(
  (set, get) => ({
    flowId: null,
    projectId: null,

    loading: false,
    visible: false,
    selection: null,
    nodes: [],
    originNodes: [],

    initial: (initialState) => {
      set(initialState);
      get().refresh();
    },
    refresh: async () => {
      const { flowId, projectId } = get();
      if (isEmpty(flowId) || isEmpty(projectId)) return;
      set({ loading: true });
      const nodes = await infoConversation(projectId, flowId);
      /// build tree and resolve 'form' and 'form-gpt' `s children. put it into a property of form.data
      const formTypes = ['form-gpt', 'form'];
      const innerTypes = ['slots-gpt', 'functions-gpt', 'slots', 'interrupt'];
      const tree: any[] = buildTree(nodes);
      treeForEach(tree, (node) => {
        if (formTypes.includes(node.type)) {
          const innerNodes = flattenTree(
            node.children?.filter((c) => innerTypes.includes(c.type)) || []
          );
          const map = keyBy(innerNodes, 'id');
          node.children = node.children?.filter((c) => !map[c.id]);
          node.data.children = innerNodes;
        }
      });
      set({ loading: false, nodes: flattenTree(tree), originNodes: nodes });
    },
    setSelection: (selection) => set({ selection }),
    refreshNodes: () => set((s) => ({ nodes: s.nodes.slice() })),
  }),
  shallow
);

export { store, store as useGraphStore };
