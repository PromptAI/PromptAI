import { BotResponse } from '@/graph-next/type';
import { BotResponseImageContent } from '@/graph-next/type';
import { BotResponseBaseContent } from '@/graph-next/type';

type EID = string;
export type ReplyVariableResponse =
  | BotResponseBaseContent
  | BotResponseImageContent;
export type ReplyVariable = {
  id: string;
  entities: Record<EID, string>;
  responses: BotResponse<ReplyVariableResponse>[];
};
export interface DataType {
  [key: string]: string | ReplyVariable['responses'];
}
export interface KeyOption {
  label: string;
  value: string;
}
