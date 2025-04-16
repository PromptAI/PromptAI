import { Slot } from '@/graph-next/type';

export interface SetSlotsFormItemProps {
  fields: {
    key: number;
    field: string;
  }[];
  operation: {
    add: (defaultValue?: any, index?: number) => void;
    remove: (index: number) => void;
    move: (fromIndex: number, toIndex: number) => void;
  };
  disabled?: boolean;
  slots?: Slot[];
}
