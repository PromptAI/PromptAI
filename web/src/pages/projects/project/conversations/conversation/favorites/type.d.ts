import React, { CSSProperties } from 'react';
import { FavoriteStoreItem } from './context/types';

type ExtraRender = (frame: FavoriteStoreItem) => React.ReactNode;
interface Extra {
  extraRender?: ExtraRender;
  detailRender?: ExtraRender;
}
export type Type = 'faq-root' | 'conversation';

export interface FavoritesType {
  type: Type;
}
export interface FavoritesProps extends FavoritesType, Extra {
  children: React.ReactNode;
  triggerStyle?: CSSProperties;
  contentStyle?: CSSProperties;
}

export interface FramesProps extends Extra {
  onVisibleChange: (v: boolean) => void;
}

export interface FrameProps extends FavoritesType, Extra {
  frame: FavoriteStoreItem;
}
