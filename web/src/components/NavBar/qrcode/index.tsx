import { Image, Modal } from '@arco-design/web-react';
import { IconWechat } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import IconButton from '../IconButton';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const Qrcode = () => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  return (
    <div>
      <IconButton
        icon={<IconWechat />}
        onClick={() => {
          setVisible(true);
        }}
      />
      <Modal
        title={t['qrcode.title']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={() => setVisible(false)}
        cancelButtonProps={{ style: { display: 'none' } }}
        unmountOnExit
        okText={t['qrcode.close']}
      >
        <Image
          width="100%"
          src={`/api/blobs/group/qrcode?type=wechat&seed=${Date.now()}`}
          alt="lamp"
        />
      </Modal>
    </div>
  );
};

export default Qrcode;
