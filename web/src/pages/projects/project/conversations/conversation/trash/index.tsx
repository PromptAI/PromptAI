import { Button } from '@arco-design/web-react';
import React from 'react';
import { ContextProvider, useTrash } from './context';
import Frames from './Frames';
import styles from './history.module.less';
import TrashSvg from '@/assets/trash.svg';

const Trash = ({ children }) => {
  const { visible, setVisible } = useTrash();
  return (
    <>
      {children}
      {visible ? (
        <div className={styles['history-container-opener']}>
          <div>
            <Frames onVisibleChange={setVisible} />
          </div>
        </div>
      ) : (
        <div className={styles['history-container-trigger']}>
          <Button
            type="primary"
            size="large"
            style={{ width: 54, height: 54 }}
            icon={<TrashSvg fontSize={32} className="arco-icon" fill="white" />}
            onClick={() => setVisible(true)}
            shape="circle"
          />
        </div>
      )}
    </>
  );
};

export default ({ children }) => (
  <ContextProvider>
    <Trash>{children}</Trash>
  </ContextProvider>
);
