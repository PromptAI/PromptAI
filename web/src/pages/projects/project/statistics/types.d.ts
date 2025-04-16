import { SummaryData } from './components/Summary';

export interface DateRangeProps {
  startTime: number;
  endTime: number;
}
export type SummaryData = {
  conversation: string;
  faq: string;
  knowledgeBase: string;
  fallback: string;
  chat: string;
  message: string;
};
export interface SummaryWithDateRangeProps extends DateRangeProps {
  summary?: SummaryData;
}
