import CodeEditor from '@/components/CodeEditor';
import { BotResponse, BotResponseActionContent } from '@/graph-next/type';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Form,
  FormInstance,
  Input,
  Link,
  Modal,
  Space,
  Typography,
} from '@arco-design/web-react';
import {
  IconCode,
  IconEdit,
  IconQuestionCircle,
} from '@arco-design/web-react/icon';
import React, { useEffect, useRef, useState } from 'react';
import i18n from './locale';
import useRules from '@/hooks/useRules';
import useDocumentLinks from '@/hooks/useDocumentLinks';

interface ActionProps {
  value?: BotResponse<BotResponseActionContent>;
  onChange?: (value: BotResponse<BotResponseActionContent>) => void;
  disabled?: boolean;
  placeholder?: string;
  defaultCode?: string;
}
const Action = ({
  value,
  onChange,
  disabled,
  placeholder,
  defaultCode,
}: ActionProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const rules = useRules();
  const formRef = useRef<FormInstance>();

  useEffect(() => {
    if (visible) {
      setTimeout(() => {
        formRef.current.setFieldValue('code', value?.content.code);
      });
    }
  }, [value?.content.code, visible]);

  const handleOK = async () => {
    const values = await formRef.current.validate();
    onChange({ ...value, content: values });
    setVisible(false);
  };
  const docs = useDocumentLinks();
  return (
    <Card
      size="small"
      bodyStyle={{ padding: 2 }}
      headerStyle={{ padding: 2, height: 'max-content' }}
    >
      <div
        className="w-full flex gap-2 items-center"
        style={{ paddingLeft: 14 }}
      >
        <Space className="flex-1">
          <Typography.Text type="primary">
            <IconCode />
          </Typography.Text>
          <Typography.Text
            type="primary"
            ellipsis={{ showTooltip: true }}
            style={{ margin: 0, maxWidth: 300 }}
          >
            {value?.content?.text || placeholder || 'action'}
          </Typography.Text>
        </Space>
        {!disabled && (
          <Button icon={<IconEdit />} onClick={() => setVisible(true)} />
        )}
      </div>
      {!disabled && (
        <Modal
          style={{ width: '60%' }}
          title={t['conversation.botForm.action']}
          visible={visible}
          onCancel={() => setVisible(false)}
          onOk={handleOK}
          unmountOnExit
        >
          <Form layout="vertical" ref={formRef} initialValues={value?.content}>
            <Form.Item
              label={t['conversation.botForm.action.text']}
              field="text"
              extra={t['conversation.botForm.action.text.help']}
            >
              <Input
                placeholder={t['conversation.botForm.action.text.placeholder']}
              />
            </Form.Item>
            <Form.Item
              label={
                <Space>
                  {t['conversation.botForm.action.code']}
                  <Link target="_blank" href={docs.botAction}>
                    <IconQuestionCircle />
                  </Link>
                </Space>
              }
              field="code"
              rules={rules}
              initialValue={defaultCode}
            >
              <CodeEditor
                width="100%"
                height={340}
                language="python"
                options={{ minimap: { enabled: false } }}
              />
            </Form.Item>
          </Form>
        </Modal>
      )}
    </Card>
  );
};

export default Action;
