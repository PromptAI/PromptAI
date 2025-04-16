import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import {
  Avatar,
  Button,
  Form,
  Input,
  Message,
  Space,
  Upload,
} from '@arco-design/web-react';
import { UploadItem } from '@arco-design/web-react/es/Upload';
import React, { useMemo } from 'react';

import i18n from './locale';

interface UploadSdkAvatarProps {
  alt: React.ReactNode;
  value?: string;
  onChange?: (value: string) => void;
  error?: boolean;
}
const UploadSdkAvatar: React.FC<UploadSdkAvatarProps> = ({
  alt,
  value,
  onChange,
  error,
}) => {
  const t = useLocale(i18n);
  const headers = useMemo(() => ({ Authorization: Token.get() }), []);

  const onFileChange = async (_, upload: UploadItem) => {
    if (upload.status === 'done') {
      const { id } = (upload.response as any) || {};
      if (id) {
        const avatar = `/api/blobs/get/${id}`;
        onChange(avatar);
      }
    }
    if (upload.status === 'error') {
      Message.error(t['config.icon.upload.error']);
    }
  };

  return (
    <Space align="start" size="large">
      <Form.Item label={t['config.icon.bot.input']} style={{ marginBottom: 0 }}>
        <Input
          placeholder={t['config.icon.bot.input.placeholder']}
          value={value}
          onChange={onChange}
        />
      </Form.Item>
      <Form.Item
        label={t['config.icon.bot.upload.label']}
        style={{ marginBottom: 0 }}
      >
        <Space>
          <Avatar
            size={34}
            autoFixFontSize={false}
            style={{ background: 'transparent' }}
          >
            {!value && !error && alt}
            {value && !error && <img src={value} alt="avatar" />}
            {error && alt}
          </Avatar>
          <Upload
            accept="image/*"
            action="/api/blobs/upload"
            showUploadList={false}
            headers={headers}
            onChange={onFileChange}
          >
            <Button type="outline" size="small">
              {t['config.icon.bot.upload']}
            </Button>
          </Upload>
        </Space>
      </Form.Item>
    </Space>
  );
};

export default UploadSdkAvatar;
