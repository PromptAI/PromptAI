import { Descendant, BaseEditor } from 'slate';
import { ReactEditor } from 'slate-react';
import { HistoryEditor } from 'slate-history';

export type ParagraphElement = { type: 'paragraph'; children: Descendant[] };
export type ButtonElement = { type: 'button'; children: Descendant[] };
export type DefaultText = {
  text: string;
};

export type SystemEditor = BaseEditor & ReactEditor & HistoryEditor;

export type CombinedElement = ParagraphElement | ButtonElement;

declare module 'slate' {
  interface CustomTypes {
    Editor: SystemEditor;
    Element: CombinedElement;
    Text: DefaultText;
  }
}
