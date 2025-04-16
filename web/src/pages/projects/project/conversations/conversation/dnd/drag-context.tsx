import { Spin } from '@arco-design/web-react';
import React, {
  createContext,
  Dispatch,
  SetStateAction,
  useContext,
  useMemo,
  useState,
} from 'react';
import { DragItem } from '../types';

interface DragContextValue {
  dragItem: DragItem;
  setDragItem: Dispatch<SetStateAction<DragItem>>;
  droping: boolean;
  setDroping: Dispatch<SetStateAction<boolean>>;
}
const DragContext = createContext<DragContextValue>({
  dragItem: null,
  setDragItem: () => void 0,
  droping: false,
  setDroping: () => void 0,
});

export function DragContextProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [dragItem, setDragItem] = useState<DragItem>(null);
  const [droping, setDroping] = useState(false);
  return (
    <Spin loading={droping} tip="..." className="w-full h-full">
      <DragContext.Provider
        value={useMemo(
          () => ({ dragItem, setDragItem, droping, setDroping }),
          [dragItem, droping]
        )}
      >
        {children}
      </DragContext.Provider>
    </Spin>
  );
}
export function useDragContext() {
  const context = useContext(DragContext);
  return context;
}
