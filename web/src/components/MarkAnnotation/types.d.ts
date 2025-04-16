import { Example, Mark } from '@/graph-next/type';

export type { Mark };
export interface MarkAnnotationValue extends Example {
  autoFocus?: boolean;
}
export interface MarkAnnotationProps {
  disabled?: boolean;
  value?: MarkAnnotationValue;
  onChange?: (value: MarkAnnotationValue) => void;
  placeholder?: string;
  slots?: any[];
  entities: (
    | {
        id: string;
        slotId: string;
        slotDisplay: string;
        slotName: string;
      }
    | any
  )[];
}
