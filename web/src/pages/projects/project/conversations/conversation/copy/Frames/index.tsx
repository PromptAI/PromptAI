import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { Button, Card, Empty, Space } from '@arco-design/web-react';
import { IconDelete, IconMinusCircle } from '@arco-design/web-react/icon';
import React from 'react';
import { useCopy } from '../context';
import Frame from './Frame';
import styles from './style.module.less';

const Frames = ({ onVisibleChange }) => {
  const { data, clear } = useCopy();
  const t = useLocale(i18n);
  return (
    <div className={styles['container']}>
      <Card
        title={t['copy.frame.title']}
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
            <Button type="text" icon={<IconDelete />} onClick={clear}>
              {t['copy.frame.clear']}
            </Button>
            <Button
              type="text"
              status="warning"
              icon={<IconMinusCircle />}
              onClick={() => onVisibleChange(false)}
            >
              {t['copy.frame.min']}
            </Button>
          </Space>
        }
      >
        <div className={styles['content']}>
          {data ? (
            <Frame frame={data} />
          ) : (
            <Empty description={t['copy.frame.empty.data']} />
          )}
        </div>
      </Card>
    </div>
  );
};

export default Frames;
