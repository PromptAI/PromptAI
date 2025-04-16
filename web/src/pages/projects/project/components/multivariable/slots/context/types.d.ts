import { Slot } from '@/graph-next/type';
import { Dictionary } from 'lodash';

export interface SlotsContextValue {
  slots: Slot[];
  map: Dictionary<Slot>;
  loading: boolean;
  operating: boolean;
  refresh: () => void;
  create: (name: string, display?: string) => Promise<Slot>;
}
