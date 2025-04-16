import CodeEditor from '@/components/CodeEditor';
import useLocale from '@/utils/useLocale';
import { Button, Modal, Space } from '@arco-design/web-react';
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
}

const CodeField = ({
  value,
  onChange,
  disabled,
  defaultValue,
  title,
  titleLink,
}: CodeFieldProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const handleOK = async () => {
    setVisible(false);
  };
  return (
    <>
      <div className="flex items-center py-1 px-2 bg-[var(--color-fill-2)]">
        <div className="flex-1 min-w-0 flex items-center space-x-1">
          <IconCode />
          <div
            className="flex-1 truncate"
            title={value ? t['drawer.field.form.code.used'] : ''}
          >
            {value ? t['drawer.field.form.code.used'] : '-'}
          </div>
        </div>
        {!disabled && (
          <Button
            type="text"
            icon={<IconEdit />}
            onClick={() => setVisible(true)}
          />
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
    </>
  );
};

export default CodeField;
