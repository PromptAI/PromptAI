import useLocale from '@/utils/useLocale';
import {
  Space,
  Divider,
  Switch,
  Form,
  SwitchProps,
  Card,
  Alert,
  Message,
  Button,
  Input,
  Radio
} from '@arco-design/web-react';
import React, { useEffect, useMemo, useState } from 'react';
import i18n from './locale';
import {
  useProjectContext,
  useProjectType
} from '@/layout/project-layout/context';
import useFormRef from '@/hooks/useFormRef';
import { updateProject } from '@/api/projects';
import styled from 'styled-components';
import { UserState, useSelectorStore } from '@/store';
import { getLLmConfig, updateLLmConfig } from '@/api/configuration';

const FAQConfigContainer = styled.div`
    padding: 8px 16px;
    border-radius: 4px;
    background: var(--color-fill-1);
`;

const DividerWrapper = styled(Divider)`
    font-weight: 600;
    margin: 10px !important;
`;

const SwitchWrapper = ({ children, ...props }: SwitchProps) => {
  return (
    <Space align="start">
      <Switch size="small" {...props} />
      <div>{children}</div>
    </Space>
  );
};

const LLMPage: React.FC = () => {
  const t = useLocale(i18n);
  const dt = useLocale();
  const userInfo = useSelectorStore<UserState>('user');
  const { refresh, ...data } = useProjectContext();

  const [faqNlpModel, setFaqNlpModel] = useState<boolean>(false);

  const defaultType = 'open_ai';
  const initialData = useMemo(() => ({ ...data, llmType: defaultType }), [data]);

  const formRef = useFormRef(initialData);


  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getLLmConfig().then((res) => {
      const type = res.type;
      if (type === 'open_ai') {
        formRef.current.setFieldValue('llmType', 'open_ai');
        formRef.current.setFieldValue('apiKay', res.openai.apiKey);
      } else {
        formRef.current.setFieldValue('llmType', 'system');
      }
    });
  }, []);

  const onSave = async () => {
    const values = await formRef.current.validate();
    setLoading(true);
    const llmType = values.llmType;
    const apiKey = values.apiKay;
    const llmParam = {
      'type': llmType,
      'openai': {
        'apiKey': apiKey
      }
    };
    try {
      await updateLLmConfig(llmParam);
      Message.success(dt['message.update.success']);
      refresh();
    } finally {
      setLoading(false);
    }

  };
  return (
    <Card
      size="small"
      title={t['sample.form.useNlpModel']}
      extra={
        <Button loading={loading} type="primary" onClick={onSave}>
          {t['llm.save']}
        </Button>
      }
      style={{ width: 600 }}
    >
      <Form ref={formRef}>
        <DividerWrapper orientation="left">{t['llm.account']}</DividerWrapper>
        <Card size="small" bordered={false}>
          <Form.Item noStyle field="llmType">
            <Radio.Group name="type">
              <Radio value="open_ai">{t['llm.account.user']}</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item shouldUpdate noStyle>
            {({ llmType }) => (
              <div style={{ marginTop: 16 }}>
                {llmType === 'open_ai' && (
                  <Form.Item
                    label={t['llm.account.user.api']}
                    field="apiKay"
                    layout="vertical"
                    required={true}
                    rules={[
                      {
                        required: true,
                        message: t['llm.account.user.api.required'] || 'API Key is required'
                      }
                    ]}
                  >
                    <Input.TextArea
                      autoSize
                      placeholder={t['llm.account.user.api.placeholder']}
                      style={{ minHeight: 64 }}
                    />
                  </Form.Item>
                )}
              </div>
            )}
          </Form.Item>
        </Card>
      </Form>
    </Card>
  );
};

export default LLMPage;
