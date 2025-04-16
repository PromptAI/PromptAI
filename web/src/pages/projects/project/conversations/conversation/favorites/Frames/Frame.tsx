import { Button, Card, Modal, Space, Typography } from '@arco-design/web-react';
import { IconShareAlt } from '@arco-design/web-react/icon';
import React, { useCallback, useMemo, useState } from 'react';
import GraphCore from '@/core-next';
import { useDragContext } from '../../dnd/drag-context';
import getNodes from '../../nodes';
import { nodeIconsMap } from '../../nodes/config';
import styles from './style.module.less';
import { FrameProps } from '../type';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const nodes = getNodes({}, true);
const FrameGraph = ({ items }) => {
  const value = useMemo(
    () =>
      items.map((f) => ({
        ...f,
        id: 'id_' + f.id,
        parentId: f.parentId ? 'id_' + f.parentId : f.parentId,
      })),
    [items]
  );
  return (
    <GraphCore
      name="favorites-graph"
      width="100%"
      height="100%"
      value={value}
      nodes={nodes}
    />
  );
};

const FrameTitle = ({ frame }) => (
  <Space>
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
      {frame.title || '-'}
    </Typography.Text>
  </Space>
);
const Frame = ({ frame, extraRender, detailRender, type }: FrameProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const { setDragItem } = useDragContext();
  const onDragStart = useCallback(
    (evt) => {
      evt.stopPropagation();
      setDragItem({ type: 'favorite_frame', item: frame });
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
  const extra = useMemo(() => extraRender?.(frame), [extraRender, frame]);
  const detail = useMemo(() => detailRender?.(frame), [detailRender, frame]);
  return (
    <Card
      size="small"
      extra={
        <Space>
          {detail || (
            <Button size="mini" type="text" onClick={() => setVisible(true)}>
              {t['favorites.frame.detail']}
            </Button>
          )}
          {extra}
        </Space>
      }
      title={
        type === 'conversation' ? (
          <div
            className={styles['drag-pointer']}
            draggable
            onDragStart={onDragStart}
            onDragEnd={onDragEnd}
          >
            <FrameTitle frame={frame} />
          </div>
        ) : (
          <FrameTitle frame={frame} />
        )
      }
      headerStyle={{
        borderBottom: '1px solid var(--color-neutral-3)',
        padding: '0px 8px',
      }}
      bodyStyle={{ padding: 0 }}
    >
      {!detail && (
        <Modal
          visible={visible}
          onCancel={() => setVisible(false)}
          title={t['favorites.frame.detail']}
          style={{ width: '50%' }}
          unmountOnExit
          footer={
            <Button type="secondary" onClick={() => setVisible(false)}>
              {t['favorites.frame.close']}
            </Button>
          }
        >
          <div style={{ height: 480 }}>
            <FrameGraph items={frame.data.items} />
          </div>
        </Modal>
      )}
    </Card>
  );
};

export default Frame;
