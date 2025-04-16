import { useControllableValue, useMemoizedFn } from 'ahooks';
import React, { CSSProperties, useCallback } from 'react';
import {
  DragDropContext,
  Droppable,
  Draggable,
  DropResult,
  DraggingStyle,
  NotDraggingStyle,
} from 'react-beautiful-dnd';

type RenderType = string;
export interface SortableItem {
  id: string | number;
  type: RenderType;
  content: any;
}
interface DndSortableListProps {
  value?: SortableItem[];
  onChange?: (values: any) => void;
  itemProps?: any;
  itemRenderMap?: Record<RenderType, (props: any) => JSX.Element>;
}
const reorder = (
  list: SortableItem[],
  startIndex: number,
  endIndex: number
): SortableItem[] => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);
  return result;
};
const grid = 8;
const getListStyle = (isDraggingOver: boolean): CSSProperties => ({
  background: isDraggingOver ? 'rgb(var(--gray-3))' : 'rgb(var(--gray-1))',
  padding: `${grid}px ${grid / 2}px`,
  width: '100%',
});
const getItemStyle = (
  isDragging: boolean,
  draggableStyle: DraggingStyle | NotDraggingStyle
): CSSProperties => ({
  // some basic styles to make the items look a bit nicer
  userSelect: 'none',
  margin: `0 0 ${grid}px 0`,
  // change background colour if dragging
  background: isDragging ? 'lightgreen' : 'rgb(var(--gray-2))',
  // styles we need to apply on draggables
  ...draggableStyle,
});

const DndSortableList = (props: DndSortableListProps) => {
  const [items, setItems] = useControllableValue<SortableItem[]>({
    value: props.value,
    onChange: props.onChange,
  });
  const onDragEnd = useMemoizedFn((result: DropResult) => {
    // dropped outside the list
    if (!result.destination) {
      return;
    }
    setItems((d) => reorder(d, result.source.index, result.destination.index));
  });
  const renderItem = useCallback(
    (item: SortableItem): React.ReactNode => {
      const Component = props.itemRenderMap?.[item.type];
      if (Component) {
        return <Component key={item.id} {...item} {...props.itemProps} />;
      }
      return <>{item.content}</>;
    },
    [props.itemRenderMap, props.itemProps]
  );
  return (
    <DragDropContext onDragEnd={onDragEnd}>
      <Droppable droppableId="droppable-sort-list">
        {(provider, snapshot) => (
          <div
            {...provider.droppableProps}
            ref={provider.innerRef}
            style={getListStyle(snapshot.isDraggingOver)}
          >
            {items.map((item, index) => (
              <Draggable key={item.id} draggableId={`${item.id}`} index={index}>
                {(provided, snap) => (
                  <div
                    ref={provided.innerRef}
                    {...provided.draggableProps}
                    {...provided.dragHandleProps}
                    style={getItemStyle(
                      snap.isDragging,
                      provided.draggableProps.style
                    )}
                  >
                    {renderItem(item)}
                  </div>
                )}
              </Draggable>
            ))}
            {provider.placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>
  );
};

export default DndSortableList;
