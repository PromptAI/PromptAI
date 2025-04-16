import { clearTrash } from '@/api/trash';
import i18n from './locale';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Empty,
  Message,
  Space,
  Spin,
} from '@arco-design/web-react';
import { IconDelete, IconMinusCircle } from '@arco-design/web-react/icon';
import React, { useCallback, useState } from 'react';
import { useTrash } from '../context';
import Frame from './Frame';
import styles from './styles.module.less';
import ru, { RU_TYPE } from '../../features/ru';

const Frames = ({ onVisibleChange }) => {
  const t = useLocale(i18n);
  const { loading, items, refreshTrash } = useTrash();
  const { projectId, flowId } = useUrlParams();
  const [clearLoading, setClearLoading] = useState(false);
  const clearAll = useCallback(() => {
    if (items.length > 0) {
      setClearLoading(true);
      // todo ru
      clearTrash(
        projectId,
        flowId,
        items.map((i) => i.key)
      )
        .then((nodes) => {
          Message.success(t['trash.frame.clear.success']);
          refreshTrash();
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
        .finally(() => setClearLoading(false));
    }
  }, [flowId, items, projectId, refreshTrash, t]);
  return (
    <div className={styles['container']}>
      <Card
        title={t['trash.frame.name']}
        bordered={false}
        headerStyle={{
          borderBottom: '1px solid var(--color-neutral-3)',
        }}
        bodyStyle={{
          maxHeight: 698,
          overflow: 'auto',
          background: 'var(--color-neutral-1)',
        }}
        extra={
          <Space>
            <Button
              loading={clearLoading}
              type="text"
              icon={<IconDelete />}
              onClick={clearAll}
            >
              {t['trash.frame.clear']}
            </Button>
            <Button
              type="text"
              status="warning"
              icon={<IconMinusCircle />}
              onClick={() => onVisibleChange(false)}
            >
              {t['trash.frame.min']}
            </Button>
          </Space>
        }
      >
        <Spin loading={loading}>
          {items.map((item) => (
            <div className={styles['content']} key={item.key}>
              <Frame frame={item} refresh={refreshTrash} />
            </div>
          ))}
          {!items.length && (
            <div className={styles['content']}>
              <Empty />
            </div>
          )}
        </Spin>
      </Card>
    </div>
  );
};

export default Frames;
