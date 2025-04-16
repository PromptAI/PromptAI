import { Dispatch, SetStateAction } from 'react';

export interface RoBotProps {
  width: string | number;
  height: string | number;
  authtication: any;
  sessionParams?: {
    scene: 'publish_db' | 'publish_snapshot' | 'debug';
    componentId?: string;
  };
  mask?: React.ReactNode;
  onAfterSendMessage?: (message: string) => void;
  className?: string;
  maskClassName?: string;
  onClose?: () => void;
  disabled?: boolean;
}
export interface ButtonItem {
  payload: string;
  title: string;
  order?: number;
}
export type CurrentPage = {
  current: number;
  pageMap: Map<number, Message>;
};
export interface RobotButtonsProps {
  buttons: ButtonItem[];
  mask?: React.ReactNode;
  onClick: (payload: string, title: string) => void;
  currentPage: CurrentPage;
  setMessages: Dispatch<SetStateAction<Message[]>>;
}
export interface SimilarQuestion {
  query: string;
  id: string;
}
export interface MessageItem {
  text?: string;
  image?: string;
  buttons?: ButtonItem[];
  attachment?: { name: string; href: string; type: string; version: string };
  upload?: { file: File; status: 'init' | 'success' | 'error' };
  multiInput?: boolean;
}
export interface Message {
  id: string;
  content: MessageItem[];
  avatar: string;
  position: 'left' | 'right';
  loading?: boolean;
  time?: number;
  extra?: {
    similarQuestions?: SimilarQuestion[];
    error?: string;
    links?: { url: string }[];
  };
}
export interface DelayMessageItem {
  content: MessageItem[];
  delay: number;
  time: number;
}
