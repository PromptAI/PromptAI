import React from 'react';
import {
  Form,
  Input as InputComponent,
  Select as SelectComponent,
  Button,
  Space,
} from '@arco-design/web-react';
import { useDefaultLocale } from '@/utils/useLocale';
const Item = Form.Item;

type optionsItem = { label: string; value: any };
type ItemProps = {
  label: string;
  value?: string | number;
  formtype: string;
  options?: optionsItem[];
  field: string;
};

const FromItems = {
  Input: (props) => (
    <Item {...props} className="!m-0">
      <InputComponent allowClear {...props} />
    </Item>
  ),
  Select: (props) => {
    return (
      <Item {...props} className="!m-0">
        <SelectComponent allowClear {...props} />
      </Item>
    );
  },
};

export default function SearchFrom({
  formItem,
  callback,
  actions,
}: {
  formItem: ItemProps[];
  callback: any;
  onReset: () => void;
  actions?: React.ReactNode[];
}) {
  const dt = useDefaultLocale();
  const [form] = Form.useForm();
  const submit = () => {
    const fields = form.getFields();
    callback(fields);
  };
  return (
    <Form form={form} layout="inline" className="justify-between">
      {formItem.map((c) => {
        return (
          <div key={c.field} className="px-3 pb-3">
            {FromItems[c.formtype](c)}
          </div>
        );
      })}
      <Form.Item className="px-3 pb-3">
        <Space>
          {actions}
          <Button key="submit" type="outline" onClick={submit}>
            {dt['from.submit.text']}
          </Button>
        </Space>
      </Form.Item>
    </Form>
  );
}
