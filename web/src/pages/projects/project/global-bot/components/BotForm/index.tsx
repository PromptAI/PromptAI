import useModalForm from '@/hooks/useModalForm';
import { useProjectContext } from '@/layout/project-layout/context';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  Input,
  Message,
  Modal,
  Space,
} from '@arco-design/web-react';
import { IconEdit, IconPlus } from '@arco-design/web-react/icon';
import React, { useEffect, useMemo, useState } from 'react';
import i18n from './locale';
import useRules from '@/hooks/useRules';
import { createGlobalBot, updateGlobalBot } from '@/api/global-component';
import { BotResponseFormItem } from '../../../components/BotResponseFormItem';

const { Item } = Form;
const BotForm = ({ value, callback }: { value: any; callback: () => void }) => {
  const { id: projectId } = useProjectContext();
  const t = useLocale(i18n);
  const rules = useRules();
  const baseResponseRules = useMemo(
    () => [
      {
        required: true,
        minLength: 1,
        message: t['BotForm.baseResponseRules.message'],
      },
    ],
    [t]
  );
  const [visible, setVisible, form] = useModalForm(value?.data);

  const handleOk = async () => {
    const data = await form.validate();
    try {
      if (value?.id) {
        await updateGlobalBot(projectId, { ...value, data });
        Message.success(t['BotForm.operation.updateSuccess']);
      } else {
        await createGlobalBot(projectId, {
          data,
          type: 'bot-global',
          componentRelation: {
            usedByComponentRoots: [],
          },
        });
        Message.success(t['BotForm.operation.addSuccess']);
      }
      setVisible(false);
      callback();
    } catch (e) {
      Message.error(t['BotForm.operation.updateFail']);
    }
  };
  const [temp, setTemp] = useState(null);
  useEffect(() => {
    if (visible) {
      setTemp(value?.data);
    } else {
      setTemp(null);
    }
  }, [visible, value?.data]);
  return (
    <div>
      <Button type="text" onClick={() => setVisible(true)}>
        {value?.id ? (
          <IconEdit />
        ) : (
          <Space>
            <IconPlus />
            {t['BotForm.header.add']}
          </Space>
        )}
      </Button>
      <Modal
        style={{ width: '45%' }}
        title={value?.id ? t['BotForm.dialog.update'] : t['BotForm.dialog.add']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleOk}
        unmountOnExit
      >
        <Form
          layout="vertical"
          form={form}
          initialValues={value?.data}
          onValuesChange={(_, values) => setTemp(values)}
        >
          <Item label={t['BotForm.dialog.name']} field="name" rules={rules}>
            <Input placeholder={t['BotForm.dialog.name.placeholder']} />
          </Item>
          <Item
            label={t['BotForm.dialog.botResponse']}
            required
            field="responses"
            rules={baseResponseRules}
          >
            <Form.List field="responses">
              {(fields, operation) => (
                <BotResponseFormItem
                  fields={fields}
                  operation={operation}
                  responses={temp?.responses}
                />
              )}
            </Form.List>
          </Item>
          <Item label={t['BotForm.dialog.description']} field="description">
            <Input.TextArea
              autoSize
              placeholder={t['BotForm.dialog.description']}
            />
          </Item>
        </Form>
      </Modal>
    </div>
  );
};

export default BotForm;
