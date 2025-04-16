import { IntentMapping } from '@/graph-next/type';
import { MutableRefObject } from 'react';
import { FormInstance } from '@arco-design/web-react';

export interface MultiMappingsProps {
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
  disabledSlot?: boolean;
  formRef: MutableRefObject<FormInstance>;
  initialMappings: IntentMapping[];
  onFromEntityMappingsChange?: (mappings: IntentMapping[]) => void;
  multiple?: boolean;
  partType: boolean;
}
