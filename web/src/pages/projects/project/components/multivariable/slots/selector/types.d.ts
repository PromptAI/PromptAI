import { Slot } from '@/graph-next/type';

export type CreateMappingSlot = Pick<
  IntentMapping,
  'slotId' | 'slotName' | 'slotDisplay'
>;
export interface SelecterProps {
  disabled?: boolean;
  size?: 'small' | 'mini' | 'default' | 'large';
  value?: string;
  onChange?: (val: string) => void;
  onCreate?: (slot: CreateMappingSlot, slot: Slot) => void;
  onRemove?:() => void;
  error?: boolean;
  disabledCreate?: boolean;
  selectedKeys?: string[];
  hiddenCreator?: boolean;
  hiddenKeys?: string[];
}
