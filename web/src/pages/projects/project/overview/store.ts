import { createWithEqualityFn } from 'zustand/traditional';
import { shallow } from 'zustand/shallow';
import { isEmpty } from 'lodash';
import { listConversations, listFaqs } from '@/api/components';
import Store from './types.d';
import { infoProject } from '@/api/projects';
import build from './build';

const fetchData = async (projectId: string) => {
  const data = await Promise.all([
    infoProject(projectId),
    listConversations(projectId),
    listFaqs(projectId),
  ]);
  return build(...data);
};

const store = createWithEqualityFn<Store>(
  (set, get) => ({
    projectId: '',

    loading: false,
    nodes: [],
    selection: null,

    initial: (initialState) => {
      set(initialState);
      get().refresh();
    },
    refresh: async () => {
      const { projectId } = get();
      if (isEmpty(projectId)) return;
      set({ loading: true });
      const data = await fetchData(projectId);
      set({
        loading: false,
        nodes: data,
      });
    },
    setSelection: (selection) => set({ selection }),
    refreshNodes: () => set((s) => ({ nodes: s.nodes.slice() })),
  }),
  shallow
);

export { store, store as useGraphStore };
