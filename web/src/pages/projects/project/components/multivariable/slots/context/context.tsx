import React, { useContext } from 'react';
import { createSlotComponent, listSlotComponent } from '@/api/components';
import { Slot } from '@/graph-next/type';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import { useMemoizedFn, useRequest } from 'ahooks';
import { Dictionary, keyBy } from 'lodash';
import { createContext, PropsWithChildren, useMemo, useState } from 'react';
import { SlotsContextValue } from './types';

const SlotsContext = createContext<SlotsContextValue>(undefined);
const defaultSlots: Slot[] = [];
const defaultSlotsMap: Dictionary<Slot> = {};
const createSlot = (name: string, display?: string): Omit<Slot, 'id'> => ({
  name,
  display: display || name,
  type: 'any',
  influenceConversation: false,
  mappings: [],
  slotType: 'string',
  enum: [],
  enumEnable: false,
  defaultValue: '',
  defaultValueEnable: false,
  defaultValueType: 'set',
});
export default function SlotsProvider({
  needMap,
  children,
}: PropsWithChildren<{ needMap?: boolean }>) {
  const { projectId } = useUrlParams();
  const {
    loading,
    data: slots = defaultSlots,
    refresh,
  } = useRequest<Slot[], []>(() => listSlotComponent(projectId), {
    refreshDeps: [projectId],
  });
  const map = useMemo(
    () => (needMap ? keyBy(slots, (s) => s.id) : defaultSlotsMap),
    [slots, needMap]
  );
  const [operating, setOperating] = useState(false);
  const create = useMemoizedFn(async (name: string, display?: string) => {
    let promise: Promise<Slot>;
    setOperating(true);
    try {
      const [slot] = await createSlotComponent(
        projectId,
        createSlot(name, display)
      );
      refresh();
      promise = Promise.resolve(slot);
    } catch (e) {
      promise = Promise.reject(e);
    }
    setOperating(false);
    return promise;
  });
  const value = useMemo(
    () => ({ loading, operating, slots, map, refresh, create }),
    [create, loading, map, operating, refresh, slots]
  );
  return (
    <SlotsContext.Provider value={value}>{children}</SlotsContext.Provider>
  );
}
export function useSlotsContext() {
  const context = useContext(SlotsContext);
  if (!context) {
    throw new Error('should be in a context');
  }
  return context;
}
export { SlotsContext };
