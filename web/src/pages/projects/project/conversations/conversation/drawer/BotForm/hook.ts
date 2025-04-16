import { listSlotComponent } from '@/api/components';
import { Slot } from '@/graph-next/type';
import { useRequest } from 'ahooks';
import { keyBy } from 'lodash';
import { useMemo } from 'react';

export function useSlots(projectId) {
  const {
    loading,
    data: slots = [],
    refresh,
  } = useRequest<Slot[], []>(() => listSlotComponent(projectId), {
    refreshDeps: [projectId],
  });
  const slotMap = useMemo(() => keyBy(slots, (s) => s.id), [slots]);
  return { loading, slots, slotMap, refresh };
}
