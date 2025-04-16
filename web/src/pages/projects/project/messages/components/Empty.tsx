import useLocale from '@/utils/useLocale';
import { Space, Typography } from '@arco-design/web-react';
import { IconMessageBanned } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from '../locale';

export default function Empty() {
  const t = useLocale(i18n);
  return (
    <div
      style={{
        height: '100%',
        width: '100%',
      }}
      className="flex items-center"
    >
      <div
        style={{
          width: '100%',
          fontSize: '25px',
        }}
        className="flex justify-center items-center"
      >
        <Space direction={'vertical'} align={'center'} size={'mini'}>
          <IconMessageBanned />
          <Typography.Text style={{ fontSize: '12px' }}>
            {t['empty.tip']}
          </Typography.Text>
        </Space>
      </div>
    </div>
  );
}
