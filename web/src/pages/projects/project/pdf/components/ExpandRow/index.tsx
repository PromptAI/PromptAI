import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { Card, Form, Input, Typography } from '@arco-design/web-react';
import React, { useEffect, useRef } from 'react';
import useRules from '@/hooks/useRules';
import useForm from '@arco-design/web-react/es/Form/useForm';
import { isEmpty } from 'lodash';
import { updateLibDesc } from '@/api/text/text';
import useUrlParams from '../../../hooks/useUrlParams';

interface ExpandProps {
  row: any;
  onSuccess: () => void;
}
const ExpandRow = ({ row, onSuccess }: ExpandProps) => {
  const t = useLocale(i18n);
  const rules = useRules();
  const [form] = useForm();
  useEffect(() => {
    form.setFieldsValue(row);
  }, [form, row]);
  const { projectId } = useUrlParams();
  return (
    <Form layout="vertical" initialValues={row} form={form}>
      <Card title={t['expand.title']}>
        <Form.Item
          label={t['data.remark']}
          field="data.description"
          //  rules={rules}
        >
          <EditableRemarkRow
            projectId={projectId}
            componentId={row.id}
            refresh={onSuccess}
          />
        </Form.Item>
        <Form.Item
          label={t['expand.content']}
          field="data.content"
          rules={rules}
        >
          <Input.TextArea
            readOnly
            autoSize={{ minRows: 6, maxRows: 12 }}
            placeholder={t['expand.content.placeholder']}
          />
        </Form.Item>
      </Card>
    </Form>
  );
};

interface EditableRemarkRowProps {
  value?: string;
  onChange?: (val: string) => void;
  projectId: string;
  componentId: string;
  refresh?: () => void;
}
const EditableRemarkRow = ({
  value = '',
  onChange,
  projectId,
  componentId,
  refresh,
}: EditableRemarkRowProps) => {
  const initValueRef = useRef(value);
  const onEnd = async (text: string) => {
    if (isEmpty(text.trim())) {
      onChange?.('');
      return;
    }
    if (initValueRef.current !== text.trim()) {
      await updateLibDesc({ desc: text.trim(), projectId, componentId });
      initValueRef.current = text.trim();
      refresh?.();
    }
  };
  return (
    <Typography.Paragraph style={{ margin: 0 }} editable={{ onChange, onEnd }}>
      {value}
    </Typography.Paragraph>
  );
};
export default ExpandRow;
