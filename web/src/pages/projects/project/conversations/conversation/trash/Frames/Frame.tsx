import React, { useCallback, useEffect, useMemo, useState } from 'react';
import GraphCore from '@/core-next';
import styles from './styles.module.less';
import {
  Button,
  Card,
  Message,
  Modal,
  Space,
  Typography,
} from '@arco-design/web-react';
import { IconShareAlt } from '@arco-design/web-react/icon';
import { useDragContext } from '../../dnd/drag-context';
import { nodeIconsMap } from '../../nodes/config';
import getNodes from '../../nodes';
import { clearTrash } from '@/api/trash';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import ru, { RU_TYPE } from '../../features/ru';

const nodes = getNodes({}, true);
const FrameGraph = ({ value }) => {
  return (
    <GraphCore
      name="trash-graph"
      width="100%"
      height="100%"
      value={value}
      nodes={nodes}
    />
  );
};
const Frame = ({ frame, refresh }) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const { setDragItem } = useDragContext();
  const { projectId, flowId } = useUrlParams();
  const [delLoading, setDelLoading] = useState(false);
  const [opened, setOpened] = useState(false);
  const value = useMemo(
    () =>
      frame.data.items.map((f) => ({
        ...f,
        id: 'id_' + f.id,
        parentId: f.parentId ? 'id_' + f.parentId : f.parentId,
      })),
    [frame]
  );
  const onDragStart = useCallback(
    (evt) => {
      evt.stopPropagation();
      setDragItem({ type: 'trash_frame', item: frame });
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
  const onDel = useCallback(() => {
    setDelLoading(true);
    // todo ru
    clearTrash(projectId, flowId, [frame.key])
      .then((nodes) => {
        Message.success(t['trash.frame.del.success']);
        refresh();
        ru.push({
          type: RU_TYPE.TRASH_CLEAR,
          changed: {
            after: nodes,
          },
          dependencies: {
            projectId,
            flowId,
          },
        });
      })
      .finally(() => setDelLoading(false));
  }, [projectId, flowId, frame.key, t, refresh]);
  return (
    <Card
      size="small"
      extra={
        <Space size={4}>
          <Button size="mini" type="text" onClick={() => setVisible(true)}>
            {t['trash.frame.detail']}
          </Button>
          <Button
            loading={delLoading}
            size="mini"
            status="danger"
            type="text"
            onClick={onDel}
          >
            {t['trash.frame.del']}
          </Button>
        </Space>
      }
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
            {frame.data.breakpoint?.data?.slotName /** special slot title */ ||
              frame.data.breakpoint?.data?.name /** special form-gpt title */ ||
              frame.title /** normal title */ ||
              '-'}
          </Typography.Text>
        </div>
      }
      headerStyle={{
        borderBottom: '1px solid var(--color-neutral-3)',
        padding: '0px 8px',
      }}
      bodyStyle={{ padding: 0 }}
    >
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        title={t['trash.frame.detail']}
        style={{ width: '50%' }}
        /* Make sure to display the content after the animation is finished */
        afterOpen={() => setOpened(true)}
        afterClose={() => setOpened(false)}
        unmountOnExit
        footer={
          <Button type="secondary" onClick={() => setVisible(false)}>
            {t['trash.frame.close']}
          </Button>
        }
      >
        <div style={{ height: 480 }}>
          {opened && <FrameGraph value={value} />}
        </div>
      </Modal>
    </Card>
  );
};

export default Frame;
