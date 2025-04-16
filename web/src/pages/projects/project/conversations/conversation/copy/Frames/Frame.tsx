import { Card, Typography } from '@arco-design/web-react';
import { IconShareAlt } from '@arco-design/web-react/icon';
import React, { useCallback, useMemo } from 'react';
import { useDragContext } from '../../dnd/drag-context';
import { titleHandlerMapping } from '../../trash/context';
import { nodeIconsMap } from '../../nodes/config';
import { Item } from '../../types';
import styles from './style.module.less';

const Frame = ({ frame }: { frame: Item }) => {
  const { setDragItem } = useDragContext();
  const title = useMemo(
    () => titleHandlerMapping[frame.type]?.(frame.data.breakpoint.data) || '-',
    [frame]
  );
  const onDragStart = useCallback(
    (evt) => {
      evt.stopPropagation();
      setDragItem({
        type: 'copy_frame',
        item: frame,
      });
    },
    [frame, setDragItem]
  );
  const onDragEnd = useCallback(
    (evt) => {
      evt.stopPropagation();
      setDragItem(null);
    },
    [setDragItem]
  );
  return (
    <Card
      size="small"
      title={
        <div
          className={styles['drag-pointer']}
          draggable
          onDragStart={onDragStart}
          onDragEnd={onDragEnd}
        >
          {nodeIconsMap[frame.type] || (
            <IconShareAlt style={{ transform: 'rotate(180deg)' }} />
          )}
          <Typography.Text
            style={{
              fontSize: 14,
              fontWeight: 'normal',
              color: 'var(--color-text-2)',
              margin: 0,
              maxWidth: 220,
            }}
            ellipsis={{ showTooltip: true }}
          >
            {title}
          </Typography.Text>
        </div>
      }
      headerStyle={{
        borderBottom: '1px solid var(--color-neutral-3)',
        padding: '0px 8px',
      }}
      bodyStyle={{ padding: 0 }}
    ></Card>
  );
};

export default Frame;
