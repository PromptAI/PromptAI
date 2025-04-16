import React from 'react';
import { Item, ItemData } from '../../types';
import { Type } from '../type';

export interface FavoritesContextValue {
  loading: boolean;
  items: FavoriteStoreItem[];
  refreshFavorites: () => void;
  type: Type;
  visible: boolean;
  setVisible: React.Dispatch<React.SetStateAction<boolean>>;
}

interface FavoriteStoreItemData extends ItemData {
  items: any[];
}

export interface FavoriteStoreItem extends Item {
  title: string;
  data: FavoriteStoreItemData;
}
