import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { Card, Form, Input, Message } from '@arco-design/web-react';
import React, { useEffect, useRef, useState } from 'react';
// import useRules from '@/hooks/useRules';
import useForm from '@arco-design/web-react/es/Form/useForm';
import { updateTextLib } from '@/api/text/text';
import useUrlParams from '../../../hooks/useUrlParams';
import useRules from '@/hooks/useRules';

interface ExpandProps {
  row: any;
  onSuccess: () => void;
}
const ExpandRow = ({ row, onSuccess }: ExpandProps) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  // const rules = useRules();
  const [editabled, setEditabled] = useState(false);
  const [form] = useForm();
  useEffect(() => {
    form.setFieldsValue(row);
  }, [form, row]);
  const ref = useRef<any>();
  const rules = useRules();
  const [, setLoading] = useState(false);
  const onSumit = (values) => {
    setLoading(true);
    updateTextLib(projectId, row.id, { ...row.data, ...values.data })
      .then(() => {
        Message.success(t['expand.create.success']);
        onSuccess();
        setEditabled(false);
      })
      .finally(() => setLoading(false));
  };
  return (
    <Form layout="vertical" initialValues={row} form={form} onSubmit={onSumit}>
      <Card title={t['expand.title']}>
        <Form.Item field="data.content" rules={rules}>
          <Input.TextArea
            ref={ref}
            readOnly={!editabled}
            autoSize={{ minRows: 6, maxRows: 12 }}
            placeholder={t['expand.content.placeholder']}
          />
        </Form.Item>
      </Card>
    </Form>
  );
};

export default ExpandRow;
