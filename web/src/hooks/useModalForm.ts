import { Form } from '@arco-design/web-react';
import { useSafeState } from 'ahooks';
import { useEffect } from 'react';
import useRules from './useRules';

function removeNullFields(value: object) {
  return Object.fromEntries(
    Object.entries(value).flatMap(([k, v]) => (v ? [[k, v]] : []))
  );
}

export default (values?: any) => {
  const [visible, setVisible] = useSafeState(false);
  const [form] = Form.useForm();
  const rules = useRules();

  useEffect(() => {
    if (!visible) {
      form.resetFields();
    } else if (values) {
      form.setFieldsValue(removeNullFields(values));
    }
  }, [visible, form, values]);
  return [visible, setVisible, form, rules] as const;
};
