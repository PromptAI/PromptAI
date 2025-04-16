import { GraphNode } from '@/graph-next/type';
import React, { SetStateAction } from 'react';

export interface SelectionProps<T extends GraphNode> {
  refresh?: () => void;
  selection: T;
  onChangeSelection?: React.Dispatch<SetStateAction<GraphNode>>;
  projectId: string;
  onChange?: React.Dispatch<SetStateAction<GraphNode[]>>;
  onChangeEditSelection?: React.Dispatch<SetStateAction<GraphNode>>;
}

export interface ItemData {
  breakpoint: any;
}
export interface Item {
  key: string;
  type: string;
  data: ItemData;
}
export type DragItem = {
  type: string;
  item: Item;
};
