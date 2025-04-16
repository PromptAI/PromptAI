import React, { useMemo, useRef, useState } from 'react';
import { DataType, KeyOption } from './@types';
import {
  Button,
  Form,
  FormInstance,
  Input,
  Modal,
} from '@arco-design/web-react';
import { IconEdit } from '@arco-design/web-react/icon';
import { ID_KEY, RES_KEY } from './const';
import { BotResponseFormItem } from '../BotResponseFormItem';
import useRules from '@/hooks/useRules';
import { keyBy } from 'lodash';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const NotEntityKeys = [ID_KEY, RES_KEY];
interface EditReplyVariableProps {
  options: KeyOption[];
  initialValue: DataType;
  onSuccess: (value: DataType) => void;
}
const EditReplyVariable: React.FC<EditReplyVariableProps> = ({
  initialValue,
  onSuccess,
  options,
}) => {
  const t = useLocale(i18n);
  const [visible, setVisible] = useState(false);
  const formRef = useRef<FormInstance>();
  const rules = useRules();

  const entities = useMemo(() => {
    const keyMap = keyBy(options, 'value');
    return Object.entries(initialValue)
      .filter(([k]) => !NotEntityKeys.includes(k))
      .map(([k]) => ({ label: keyMap[k]?.label || '-', field: k }));
  }, [initialValue, options]);
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
      onSuccess({ ...data, [ID_KEY]: initialValue[ID_KEY] });
      setVisible(false);
    });
  return (
    <>
      <Button
        size="mini"
        type="primary"
        icon={<IconEdit />}
        onClick={() => setVisible(true)}
      />
      <Modal
        style={{ width: '45%' }}
        title={t['modal.title']}
        visible={visible}
        onCancel={() => setVisible((v) => !v)}
        onOk={onOk}
        unmountOnExit
        closable={false}
      >
        <Form ref={formRef} layout="vertical" initialValues={initialValue}>
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

export default EditReplyVariable;
