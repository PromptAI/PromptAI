import React, {
  ReactNode,
  cloneElement,
  useMemo,
  useRef,
  useState,
} from 'react';
import { DataType, KeyOption } from './@types';
import { Form, FormInstance, Input, Modal } from '@arco-design/web-react';
import { ID_KEY, RES_KEY } from './const';
import { BotResponseFormItem } from '../BotResponseFormItem';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { nanoid } from 'nanoid';

interface AddReplyVariableProps {
  options: KeyOption[];
  onSuccess: (value: DataType) => void;
  trigger: ReactNode;
}
const AddReplyVariable: React.FC<AddReplyVariableProps> = ({
  onSuccess,
  options,
  trigger,
}) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const formRef = useRef<FormInstance>();
  const rules = useRules();

  const entities = useMemo(() => {
    return options.map(({ label, value }) => ({
      label: label || '-',
      field: value,
    }));
  }, [options]);
  const baseResponseRules = useMemo(
    () => [
      {
        required: true,
        minLength: 1,
        message: t['message.error.notCompleted'],
      },
    ],
    [t]
  );
  const onOk = () =>
    formRef.current.validate().then((data) => {
      onSuccess({ ...data, [ID_KEY]: nanoid() });
      setVisible(false);
    });
  return (
    <>
      {cloneElement(trigger as any, { onClick: () => setVisible(true) })}
      <Modal
        style={{ width: '45%' }}
        title={t['modal.title']}
        visible={visible}
        onCancel={() => setVisible((v) => !v)}
        onOk={onOk}
        unmountOnExit
        closable={false}
      >
        <Form ref={formRef} layout="vertical">
          {entities.map(({ label, field }) => (
            <Form.Item key={field} label={label} field={field} rules={rules}>
              <Input placeholder={t['modal.variable.value.placeholder']} />
            </Form.Item>
          ))}
          <Form.Item
            label={t['table.reply']}
            field={RES_KEY}
            rules={baseResponseRules}
            onChange={() => formRef.current.validate([RES_KEY])}
          >
            {(values) => (
              <Form.List field={RES_KEY}>
                {(fields, operation) => (
                  <BotResponseFormItem
                    fields={fields}
                    operation={operation}
                    responses={values[RES_KEY] as any}
                    config={{ webhook: false, action: false }}
                  />
                )}
              </Form.List>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default AddReplyVariable;
