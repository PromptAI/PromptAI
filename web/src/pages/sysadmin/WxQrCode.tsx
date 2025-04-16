import { updateConfigurations } from '@/api/projects';
import i18n from './locale';
import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import { Button, Image, Message, Upload } from '@arco-design/web-react';
import { IconEdit } from '@arco-design/web-react/icon';
import { useCreation } from 'ahooks';
import React, { useState } from 'react';

const WxQrCode = () => {
  const headers = useCreation(() => ({ Authorization: Token.get() }), []);
  const [seed, forceUpdate] = useState(0);
  const onUploadChange = (_, upload) => {
    if (upload.status === 'done') {
      const { id } = upload.response;
      updateConfigurations({ name: 'group_qrcode_wechat', value: id }).then(
        () => {
          Message.success('success');
          setTimeout(() => forceUpdate((f) => f + 1), 200);
        }
      );
    }
    if (upload.status === 'error') {
      Message.error('error');
    }
  };
  const t = useLocale(i18n);
  return (
    <div className="flex justify-center items-end gap-2">
      <Image
        key={seed}
        width={520}
        height={640}
        src={`/api/blobs/group/qrcode?type=wechat&seed=${Date.now()}`}
        alt="lamp"
      />
      <Upload
        key="upload-image"
        accept="image/*"
        action="/api/blobs/upload"
        showUploadList={false}
        headers={headers}
        onChange={onUploadChange}
      >
        <Button size="large" type="primary" icon={<IconEdit />}>
          {t['update']}
        </Button>
      </Upload>
    </div>
  );
};

export default WxQrCode;
