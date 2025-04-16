import useLocale from '@/utils/useLocale';
import { Button, Form, FormListProps } from '@arco-design/web-react';
import { IconDelete, IconPlus } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from './i18n';
import Example, { ExampleProps } from '../Example';

type ChildrenParams = Parameters<FormListProps['children']>;
interface ExamplesFormItemProps extends ExampleProps {
  fields: ChildrenParams[0];
  operation: ChildrenParams[1];
}
const ExamplesFormItem: React.FC<ExamplesFormItemProps> = ({
  fields,
  operation: { add, remove },
  disabled,
  ...props
}) => {
  const t = useLocale(i18n);
  return (
    <div className="w-full space-y-2">
      {fields.map((field, index) => (
        <div key={field.key} className="flex gap-2">
          <Form.Item
            key={field.key}
            field={field.field}
            className="!m-0 w-full"
          >
            <Example disabled={disabled} style={{ minHeight: 32 }} {...props} />
          </Form.Item>
          {!disabled && (
            <Button
              type="secondary"
              status="danger"
              onClick={() => remove(index)}
            >
              <IconDelete />
            </Button>
          )}
        </div>
      ))}
      {!disabled && (
        <Button
          long
          type="outline"
          status="success"
          onClick={() => add({ text: '', marks: [], autoFocus: true })}
        >
          <IconPlus />
          {t['add']}
        </Button>
      )}
    </div>
  );
};

export default ExamplesFormItem;
