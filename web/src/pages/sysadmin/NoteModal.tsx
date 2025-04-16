import { addNote } from '@/api/projects';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import { useDefaultLocale } from '@/utils/useLocale';
import {
  Modal,
  Button,
  Form,
  Input,
  Message,
  Tooltip,
} from '@arco-design/web-react';
import { IconFile } from '@arco-design/web-react/icon';
import React, { useState } from 'react';

const { Item } = Form;

export default function NoteModal({ initialValues, callback }) {
  const [visible, setVisible, form] = useModalForm(initialValues);
  const rules = useRules();
  const dt = useDefaultLocale();

  const [loading, setloading] = useState(false);

  const onOk = () => {
    form.validate().then((res) => {
      setloading(true);
      addNote({ ...res, accountExtId: initialValues.id })
        .then(() => {
          Message.success(dt['message.create.success']);
          callback();
          setVisible(false);
        })
        .finally(() => setloading(false));
    });
  };
  return (
    <>
      <Tooltip content={dt['sysadmin.table.operation.note']}>
        <Button
          icon={<IconFile />}
          type="text"
          size="small"
          onClick={() => setVisible(true)}
        >
          {/* {dt['sysadmin.table.operation.note']} */}
        </Button>
      </Tooltip>
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        title={dt['sysadmin.create.note.form']}
        confirmLoading={loading}
      >
        <Form form={form} layout={'vertical'}>
          <Item label={dt['sysadmin.form.note']} field={'note'} rules={rules}>
            <Input.TextArea placeholder={dt['from.input.placeholder']} />
          </Item>
        </Form>
      </Modal>
    </>
  );
}
