import { Button } from '@arco-design/web-react';
import React from 'react';
import { FiClipboard } from 'react-icons/fi';
import CopyContextProvider, { useCopy } from './context';
import styles from './style.module.less';
import Frames from './Frames';

const Copy = ({ children }) => {
  const { visible, setVisible } = useCopy();
  return (
    <div style={{ position: 'relative' }}>
      {children}
      {visible && (
        <div className={styles['copy-container-opener']}>
          <Frames onVisibleChange={setVisible} />
        </div>
      )}
      <div className={styles['copy-container-trigger']}>
        <Button
          type="primary"
          size="large"
          status={visible ? 'success' : 'default'}
          style={{ width: 54, height: 54 }}
          icon={<FiClipboard fontSize={32} />}
          onClick={() => setVisible(true)}
          shape="circle"
        />
      </div>
    </div>
  );
};

export default ({ children, flowId }) => (
  <CopyContextProvider flowId={flowId}>
    <Copy>{children}</Copy>
  </CopyContextProvider>
);
