import React, { useState } from 'react';
import { Button, Modal } from '@arco-design/web-react';
import AioInstallCommand from './AioInstallCmd';

const InstallCmd = ({ row, ...props }) => {
  const [visible, setVisible] = useState(false);

  return (
    <>
      <Button {...props} onClick={() => setVisible(true)} />
      <Modal
        style={{ width: '45%' }}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={() => setVisible(false)}
      >
        <AioInstallCommand cmd={row.installCmd} />
      </Modal>
    </>
  );
};

export default InstallCmd;
