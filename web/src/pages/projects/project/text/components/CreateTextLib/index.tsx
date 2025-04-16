import { createTextLib } from '@/api/text/text';
import useModalForm from '@/hooks/useModalForm';
import useLocale from '@/utils/useLocale';
import { Button, Form, Input, Message, Modal } from '@arco-design/web-react';
import { IconPlus } from '@arco-design/web-react/icon';
import React from 'react';
import useUrlParams from '../../../hooks/useUrlParams';
import i18n from './locale';

interface CreateTextLibProps {
  onSuccess: () => void;
}
const CreateTextLib = ({ onSuccess }: CreateTextLibProps) => {
  const t = useLocale(i18n);
  const [visible, setVisible, form, rules] = useModalForm();
  const { projectId } = useUrlParams();
  const onSubmit = async () => {
    const values = await form.validate();
    await createTextLib(projectId, values);
    Message.success(t['create.success']);
    onSuccess();
    setVisible(false);
  };
  return (
    <div>
      <Button
        type="primary"
        icon={<IconPlus />}
        onClick={() => setVisible(true)}
      >
        {t['create.title']}
      </Button>
      <Modal
        style={{ width: '50%' }}
        title={t['create.title']}
        unmountOnExit
        visible={visible}
        onOk={onSubmit}
        onCancel={() => setVisible(false)}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label={t['create.remark']} field="description">
            <Input placeholder={t['create.remark']} />
          </Form.Item>
          <Form.Item label={t['create.content']} field="content" rules={rules}>
            <Input.TextArea
              autoSize={{ minRows: 12 }}
              placeholder={t['create.content.placeholder']}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CreateTextLib;
