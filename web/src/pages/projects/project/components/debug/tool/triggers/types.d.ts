import { ReactNode } from 'react';

export interface DebugRunProps {
  title: string;
  icon?: ReactNode;
  current: string;
  start: (componentIds: string[], projectId: string) => void;
}
export interface DebugOption {
  label: string;
  value: string;
  disabled: boolean;
  type: 'conversation' | 'faq-root';
}
export interface DebugData {
  loading: boolean;
  options: DebugOption[];
}
