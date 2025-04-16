import useRules from '@/hooks/useRules';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Card,
  Form,
  Input,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
} from '@arco-design/web-react';
import {
  IconClose,
  IconPlus,
  IconQuestionCircleFill,
} from '@arco-design/web-react/icon';
import React from 'react';
import { ConditionsFormItemProps } from './types';

const { Item } = Form;
const ConditionsFormItem = ({
  fields,
  operation: { add, remove },
  disabled,
  slots,
}: ConditionsFormItemProps) => {
  const t = useLocale(i18n);
  const rules = useRules();

  const conditionTypes = [
    { label: t['conversation.botForm.more.actions.condition.type.isEmpty'], value: 'isEmpty' },
    { label: t['conversation.botForm.more.actions.condition.type.isNotEmpty'], value: 'isNotEmpty' },
    { label: t['conversation.botForm.more.actions.condition.type.notEqual'], value: 'notEqual' },
    { label: t['conversation.botForm.more.actions.condition.type.equal'], value: 'equal' },
    { label: t['conversation.botForm.more.actions.condition.type.greaterThan'], value: 'greaterThan' },
    { label: t['conversation.botForm.more.actions.condition.type.greaterThanOrEqual'], value: 'greaterThanOrEqual' },
    { label: t['conversation.botForm.more.actions.condition.type.lessThan'], value: 'lessThan' },
    { label: t['conversation.botForm.more.actions.condition.type.lessThanOrEqual'], value: 'lessThanOrEqual' },
    { label: t['conversation.botForm.more.actions.condition.type.regex'], value: 'regex' },
    { label: t['conversation.botForm.more.actions.condition.type.contains'], value: 'contains' },
    { label: t['conversation.botForm.more.actions.condition.type.notContains'], value: 'notContains' },
    { label: t['conversation.botForm.more.actions.condition.type.startsWith'], value: 'startsWith' },
    { label: t['conversation.botForm.more.actions.condition.type.endsWith'], value: 'endsWith' },
  ];

  return (
    <Space direction="vertical" className="w-full">
      <Button
        long
        type="dashed"
        status="success"
        icon={<IconPlus />}
        disabled={disabled}
        onClick={() => add({ slotId: null, type: null, value: null }, 0)}
      >
        {t['conversation.botForm.more.actions.condition']}
      </Button>
      {fields.map((field, index) => (
        <Card
          key={field.key}
          title={
            <Typography.Text style={{ fontSize: 14 }}>
              {t['conversation.botForm.more.actions.condition']}
            </Typography.Text>
          }
          headerStyle={{ padding: '0 8px' }}
          bodyStyle={{ padding: 8 }}
          extra={
            <Button
              size="small"
              icon={<IconClose />}
              type="text"
              status="danger"
              onClick={() => remove(index)}
              disabled={disabled}
            >
              {t['conversation.botForm.delete']}
            </Button>
          }
        >
          <Space direction="vertical" className="w-full">
            <Item
              label={t['conversation.botForm.more.actions.reset.name']}
              field={field.field + 'slotId'}
              rules={rules}
              labelCol={{ span: 4 }}
              className="!m-0"
            >
              <Select
                allowClear
                showSearch
                disabled={disabled}
                filterOption={(inputValue, option) =>
                  option.props.extra
                    .toLowerCase()
                    .indexOf(inputValue.toLowerCase()) >= 0
                }
                placeholder={t['conversation.botForm.select.placeholder']}
              >
                {slots?.map(({ id, name, display, blnInternal }) => (
                  <Select.Option key={id} value={id} extra={display}>
                    {display || name}{' '}
                    {blnInternal && (
                      <Tag color="blue">
                        {t['conversation.botForm.select.builtin']}
                      </Tag>
                    )}
                  </Select.Option>
                ))}
              </Select>
            </Item>
            <Item
              label={t['conversation.botForm.more.actions.condition.type']}
              field={field.field + 'type'}
              rules={rules}
              labelCol={{ span: 4 }}
              className="!m-0"
            >
              <Select
                placeholder={t['conversation.botForm.select.placeholder']}
                disabled={disabled}
              >
                {conditionTypes.map(({ label, value }) => (
                  <Select.Option key={value} value={value}>
                    {label}
                  </Select.Option>
                ))}
              </Select>
            </Item>
            <Form.Item shouldUpdate noStyle>
              {(values) => {
                const currentType = values?.conditions?.[field.field.match(/\d+/)?.[0]]?.type;
                if (currentType && !['isEmpty', 'isNotEmpty'].includes(currentType)) {
                  return (
                    <Item
                      label={
                        <Space>
                          <Typography.Text>
                            {t['conversation.botForm.more.actions.condition.value']}
                          </Typography.Text>
                          <Tooltip
                            content={
                              t[
                                'conversation.botForm.more.actions.condition.value.help'
                              ]
                            }
                          >
                            <IconQuestionCircleFill />
                          </Tooltip>
                        </Space>
                      }
                      field={field.field + 'value'}
                      labelCol={{ span: 4 }}
                      className="!m-0"
                      rules={[{ required: true, message: 'Value is required' }]}
                    >
                      <Input
                        disabled={disabled}
                        placeholder={
                          t[
                            'conversation.botForm.more.actions.condition.value.placeholder'
                          ]
                        }
                      />
                    </Item>
                  );
                }
                return null;
              }}
            </Form.Item>
          </Space>
        </Card>
      ))}
    </Space>
  );
};

export default ConditionsFormItem;
