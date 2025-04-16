import { IntentMapping } from '@/graph-next/type';
import { CreateMappingSlot } from '../slots/selector/types';

export interface MappingProps {
  value?: IntentMapping;
  onChange?: (value: IntentMapping) => void;
  onCreate?: (slot: CreateMappingSlot) => void;
  onTypeChange?: (type: MappingType) => void;
  onSlotChange?: (slot: CreateMappingSlot) => void;
  onRemove? : () => void;
  disabled?: boolean;
  disabledSlot?: boolean;
  selectedKeys?: string[];
  partType: boolean;
}
export type MappingType = 'from_text' | 'from_entity' | 'from_intent';
