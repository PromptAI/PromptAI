import {
  Form,
  Space,
  Button,
  Typography,
  Card,
  Select,
  Input,
  Tag,
} from '@arco-design/web-react';
import React from 'react';
import { SetSlotsFormItemProps } from './types';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { IconClose, IconPlus } from '@arco-design/web-react/icon';

const { Item } = Form;
export type { SetSlotsFormItemProps };
const SetSlotsFormItem = ({
  fields,
  operation: { add, remove },
  disabled,
  slots,
}: SetSlotsFormItemProps) => {
  const t = useLocale(i18n);
  return (
    <Space size="small" direction="vertical" className="w-full">
      <Button
        type="dashed"
        status="success"
        long
        icon={<IconPlus />}
        disabled={disabled}
        onClick={() => add({ slotId: null, value: null }, 0)}
      >
        {t['reset.title']}
      </Button>
      {fields.map((field, index) => {
        return (
          <Card
            title={
              <Typography.Text style={{ fontSize: 14 }}>
                {t['reset.title']}
              </Typography.Text>
            }
            size="small"
            headerStyle={{ padding: '0 8px' }}
            bodyStyle={{ padding: 8 }}
            key={field.key}
            extra={
              <Button
                size="small"
                type="text"
                shape="circle"
                status="danger"
                icon={<IconClose />}
                disabled={disabled}
                onClick={() => remove(index)}
              />
            }
          >
            <Space direction="vertical" size="mini" className="w-full">
              <Item
                label={t['reset.name']}
                field={field.field + 'slotId'}
                rules={[
                  {
                    required: true,
                    message: t['reset.name.rule'],
                  },
                ]}
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
                  placeholder={t['reset.name.placeholder']}
                >
                  {slots?.map(({ id, name, display, blnInternal }) => (
                    <Select.Option key={id} value={id} extra={display}>
                      {display || name}{' '}
                      {blnInternal && (
                        <Tag color="blue">{t['reset.name.builtin']}</Tag>
                      )}
                    </Select.Option>
                  ))}
                </Select>
              </Item>
              <Item
                label={t['reset.value']}
                field={field.field + 'value'}
                labelCol={{ span: 4 }}
                className="!m-0"
              >
                <Input
                  disabled={disabled}
                  placeholder={t['reset.value.placeholder']}
                />
              </Item>
            </Space>
          </Card>
        );
      })}
    </Space>
  );
};
export default SetSlotsFormItem;
