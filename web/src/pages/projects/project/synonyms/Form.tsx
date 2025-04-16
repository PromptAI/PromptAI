import React from 'react';
import i18n from './locale';
import { isBlank } from '@/utils/is';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Divider,
  Empty,
  Form,
  Input,
  Space,
} from '@arco-design/web-react';
import { IconDelete, IconPlus } from '@arco-design/web-react/icon';

const { Item } = Form;
const SynonymForm = (props) => {
  const t = useLocale(i18n);

  const synonymsRules = [
    {
      required: true,
      minLength: 1,
      message: t['synonyms.form.synonyms.required'],
    },
  ];

  const mainExampleRules = [
    {
      validator(value, callback) {
        if (!value || isBlank(value.text)) {
          callback(t['synonyms.textRule']);
        }
      },
    },
  ];

  const onValuesChange = (_, values) => {
    props.setData(values);
  };
  return (
    <Form
      layout="vertical"
      ref={props.formRef}
      onValuesChange={onValuesChange}
      initialValues={props.data}
    >
      <Item
        label={t['synonyms.original']}
        field="original"
        rules={mainExampleRules}
        required
      >
        <Input />
      </Item>
      <Item
        label={t['synonyms.synonyms']}
        rules={synonymsRules}
        required
        field="synonyms"
        onChange={() => {
          // 内容更新时做内容校验
          props.formRef.current.validate(['synonyms']);
        }}
      >
        <Form.List field="synonyms">
          {(fields, { add, remove }) => (
            <Space direction="vertical" className="w-full">
              {fields.length === 0 && (
                <Empty description={t['synonyms.synonyms.empty']} />
              )}
              {fields.map((field, index) => (
                <div key={field.key} className="flex gap-2">
                  <Item
                    key={field.key}
                    field={field.field}
                    rules={mainExampleRules}
                    className="!m-0 w-full"
                  >
                    <Input placeholder={t['synonyms.synonyms.placeholder']} />
                  </Item>
                  <Button
                    type="outline"
                    status="danger"
                    onClick={() => remove(index)}
                  >
                    <IconDelete />
                  </Button>
                </div>
              ))}
              <Button
                long
                type="outline"
                status="success"
                onClick={() => add('')}
              >
                <IconPlus />
                {t['synonyms.add']}
              </Button>
            </Space>
          )}
        </Form.List>
      </Item>
      <Divider />
      <Item label={t['synonyms.description']} field="description">
        <Input.TextArea
          autoSize
          placeholder={t['synonyms.description.placeholder']}
        />
      </Item>
    </Form>
  );
};

export default SynonymForm;
