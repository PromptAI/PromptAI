import { getAgentTip } from '@/api/agent';
import { shallow } from 'zustand/shallow';
import { createWithEqualityFn } from 'zustand/traditional';

type State = {
  loading: boolean;
  available: boolean;

  visible: boolean;
  toggle: () => void;
};
type Action = {
  init: () => Promise<void>;
};

const useAgentStore = createWithEqualityFn<State & Action>(
  (set) => ({
    loading: false,
    available: true,
    init: async () => {
      set({ loading: true });
      try {
        const { available } = await getAgentTip();
        set({ available });
      } catch (e) {
        //not impl
      }
      set({ loading: false });
    },
    visible: false,
    toggle: () => {
      set((state) => ({ visible: !state.visible }));
    },
  }),
  shallow
);

export default useAgentStore;
