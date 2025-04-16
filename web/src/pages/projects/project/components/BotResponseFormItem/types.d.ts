import { BotDataType, BotResponse } from '@/graph-next/type';

export interface BotResponseFormItemProps {
  fields: {
    key: number;
    field: string;
  }[];
  operation: {
    add: (defaultValue?: any, index?: number) => void;
    remove: (index: number) => void;
    move: (fromIndex: number, toIndex: number) => void;
  };
  responses: BotResponse<BotDataType>[];
  config?: Partial<BotResponseConfigParams>;
  disabled?: boolean;
  isFaq?: boolean;
}
export interface ActionProps {
  visible: boolean;
  action: (v: any) => void;
}
export interface BotResponseConfigParams {
  text: boolean;
  image: boolean;
  webhook: boolean;
  attachment: boolean;
  action: boolean;
}
