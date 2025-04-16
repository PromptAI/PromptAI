import React from 'react';
import { Item } from '../../types';

export interface CopyContextValue {
  data?: Item;
  submit: (data: Item) => void;
  clear: () => void;
  visible: boolean;
  setVisible: React.Dispatch<React.SetStateAction<boolean>>;
}
