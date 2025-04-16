import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Image,
  Message,
  Space,
  Tooltip,
  Upload,
} from '@arco-design/web-react';
import { UploadItem } from '@arco-design/web-react/es/Upload';
import { IconDelete, IconEdit, IconEye } from '@arco-design/web-react/icon';
import { useCreation } from 'ahooks';
import React, { useState } from 'react';
import i18n from '@/locale';

const UploadImage = ({
  value,
  onChange,
}: {
  value?: string;
  onChange?: any;
}) => {
  const t = useLocale(i18n);
  const headers = useCreation(() => ({ Authorization: Token.get() }), []);
  const [visible, setVisible] = useState(false);

  const onFileChange = async (_, upload: UploadItem) => {
    if (upload.status === 'done') {
      const { id } = (upload.response as any) || {};
      onChange(`/api/blobs/get/${id}`);
    }
    if (upload.status === 'error') {
      Message.error(t['graph.bot.panel.image.response.error']);
    }
  };

  const onDel = () => {
    onChange(null);
  };

  return (
    <div className="arco-upload-trigger">
      <Upload
        key="upload-image"
        accept="image/*"
        action="/api/blobs/upload"
        showUploadList={false}
        headers={headers}
        onChange={onFileChange}
        style={{ width: '100%', height: 'max-content' }}
      >
        <div
          className="arco-upload-list-item-picture"
          style={{ width: '100%', height: 'max-content' }}
          onClick={(evt) => {
            evt.stopPropagation();
          }}
        >
          <Image
            width="100%"
            src={value || ''}
            previewProps={{
              visible,
              onVisibleChange: setVisible,
            }}
          />
          <div className="arco-upload-list-item-picture-mask">
            <Space style={{ height: '100%' }}>
              <Tooltip content={t['image.tooltip.preview']}>
                <Button
                  type="primary"
                  shape="circle"
                  icon={<IconEye />}
                  onClick={() => setVisible(true)}
                />
              </Tooltip>
              <Upload
                key="upload-image"
                accept="image/*"
                action="/api/blobs/upload"
                showUploadList={false}
                headers={headers}
                onChange={onFileChange}
              >
                <Tooltip content={t['image.tooltip.select']}>
                  <Button
                    type="primary"
                    shape="circle"
                    status="warning"
                    icon={<IconEdit />}
                  />
                </Tooltip>
              </Upload>
              <Tooltip content={t['image.tooltip.delete']}>
                <Button
                  type="primary"
                  shape="circle"
                  status="danger"
                  icon={<IconDelete />}
                  onClick={onDel}
                />
              </Tooltip>
            </Space>
          </div>
        </div>
      </Upload>
    </div>
  );
};

export default UploadImage;
