import { Item, ItemData } from '../../types';

export interface TrashContextValue {
  loading: boolean;
  items: TrashStoreItem[];
  refreshTrash: () => void;
  visible: boolean;
  setVisible: React.Dispatch<React.SetStateAction<boolean>>;
}
interface TrashStoreItemData extends ItemData {
  items: any[];
}
export interface TrashStoreItem extends Item {
  title: string;
  data: TrashStoreItemData;
}
