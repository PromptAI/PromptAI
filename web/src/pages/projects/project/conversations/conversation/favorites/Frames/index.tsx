import useLocale from '@/utils/useLocale';
import { Button, Card, Empty, Spin } from '@arco-design/web-react';
import { IconMinusCircle } from '@arco-design/web-react/icon';
import React from 'react';
import { useFavorites } from '../context';
import { FramesProps } from '../type';
import Frame from './Frame';
import i18n from './locale';
import styles from './style.module.less';

const Frames = ({
  onVisibleChange,
  extraRender,
  detailRender,
}: FramesProps) => {
  const { loading, items, type } = useFavorites();
  const t = useLocale(i18n);
  return (
    <div className={styles['container']}>
      <Card
        size="small"
        title={t['favorites.frame.name']}
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
          <Button
            type="text"
            size="small"
            status="warning"
            icon={<IconMinusCircle />}
            onClick={() => onVisibleChange(false)}
          >
            {t['favorites.frame.min']}
          </Button>
        }
      >
        <Spin loading={loading}>
          {items.map((item, index) => (
            <div className={styles['content']} key={item.key + index}>
              <Frame
                frame={item}
                type={type}
                extraRender={extraRender}
                detailRender={detailRender}
              />
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
