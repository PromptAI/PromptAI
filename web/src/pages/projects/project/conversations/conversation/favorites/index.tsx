import { Button } from '@arco-design/web-react';
import { IconStar } from '@arco-design/web-react/icon';
import React from 'react';
import FavoritesContextProvider, { useFavorites } from './context';
import Frames from './Frames';
import styles from './style.module.less';
import { FavoritesProps } from './type';

const Favorites = ({
  children,
  extraRender,
  detailRender,
  triggerStyle,
  contentStyle,
}: Omit<FavoritesProps, 'type'>) => {
  const { visible, setVisible } = useFavorites();
  return (
    <>
      {children}
      {visible && (
        <div className={styles['container-opener']} style={contentStyle}>
          <div>
            <Frames
              onVisibleChange={setVisible}
              extraRender={extraRender}
              detailRender={detailRender}
            />
          </div>
        </div>
      )}
      <div className={styles['container-trigger']} style={triggerStyle}>
        <Button
          type="primary"
          size="small"
          status={visible ? 'success' : 'default'}
          style={{ width: 54, height: 54 }}
          icon={<IconStar fontSize={32} />}
          onClick={() => setVisible(!visible)}
          shape="circle"
        />
      </div>
    </>
  );
};

export default ({ type, ...props }: FavoritesProps) => (
  <FavoritesContextProvider type={type}>
    <Favorites {...props} />
  </FavoritesContextProvider>
);
