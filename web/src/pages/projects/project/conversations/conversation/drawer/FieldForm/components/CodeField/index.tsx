import CodeEditor from '@/components/CodeEditor';
import useLocale from '@/utils/useLocale';
import { Button, Modal, Space, Typography } from '@arco-design/web-react';
import {
  IconCode,
  IconEdit,
  IconQuestionCircle,
} from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import i18n from './locale';

interface CodeFieldProps {
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
  defaultValue?: string;
  title: string;
  titleLink?: string;
  name?:string;
}

const CodeField = ({
  value,
  onChange,
  disabled,
  defaultValue,
  title,
  titleLink,
  name
}: CodeFieldProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const handleOK = async () => {
    setVisible(false);
  };
  return (
    <div>
      <div
        className="w-full flex gap-2 items-center"
        style={{ paddingLeft: 14 }}
      >
        <Space className="flex-1" >
          <Typography.Text type="primary">
            <IconCode />
          </Typography.Text>
          <Typography.Text
            type="primary"
            ellipsis={{ showTooltip: true }}
            style={{ margin: 0, maxWidth: 300 ,minWidth: 200}}
            onClick={() => setVisible(true)}
          >
            {name ? name : 'action'}
          </Typography.Text>
        </Space>
        {!disabled && (
          <Button icon={<IconEdit />} onClick={() => setVisible(true)} />
        )}
      </div>
      {!disabled && (
        <Modal
          style={{ width: '60%' }}
          title={
            <Space>
              {title}
              {titleLink && (
                <a target="_blank" href={titleLink} rel="noreferrer">
                  <IconQuestionCircle />
                </a>
              )}
            </Space>
          }
          visible={visible}
          onCancel={() => setVisible(false)}
          onOk={handleOK}
          unmountOnExit
        >
          <CodeEditor
            value={value}
            onChange={onChange}
            width="100%"
            height={500}
            language="python"
            defaultValue={defaultValue}
            options={{ minimap: { enabled: false } }}
          />
        </Modal>
      )}
    </div>
  );
};

export default CodeField;
