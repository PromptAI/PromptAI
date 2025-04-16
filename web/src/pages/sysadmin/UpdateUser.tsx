import { editAccount } from '@/api/projects';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import { regEmail } from '@/utils/regex';
import { useDefaultLocale } from '@/utils/useLocale';
import {
  Modal,
  Button,
  Form,
  Input,
  Select,
  Message,
  Tooltip,
  Switch,
  Space,
} from '@arco-design/web-react';
import { IconEdit, IconUser } from '@arco-design/web-react/icon';
import React, { useState } from 'react';

const { Item } = Form;
const PhoneReg = new RegExp('^[1][3,5,7,8][0-9]{9}$');
const timeList = [
  'UTC ',
  'UTC+1',
  'UTC+2 ',
  'UTC+3 ',
  'UTC+4 ',
  'UTC+5 ',
  'UTC+6 ',
  'UTC+7 ',
  'UTC+8 ',
  'UTC+9 ',
  'UTC+10 ',
  'UTC+11 ',
  'UTC+12 ',
  'UTC+13 ',
  'UTC+14 ',
  'UTC-1',
  'UTC-2 ',
  'UTC-3 ',
  'UTC-4 ',
  'UTC-5 ',
  'UTC-6 ',
  'UTC-7 ',
  'UTC-8 ',
  'UTC-9 ',
  'UTC-10 ',
  'UTC-11 ',
  'UTC-12 ',
];
interface UpdateUserProps {
  initialValues: any;
  callback?: any;
  readyOnly?: boolean;
  triggerTitle?: string;
}
export default function UpdateUser({
  initialValues,
  callback,
  readyOnly,
  triggerTitle,
}: UpdateUserProps) {
  const [visible, setVisible, form] = useModalForm(initialValues);
  const rules = useRules();
  const dt = useDefaultLocale();
  const [loading, setloading] = useState(false);

  const onOk = () => {
    if (!readyOnly) {
      form.validate().then((res) => {
        setloading(true);
        editAccount({ ...res, accountExtId: initialValues.id })
          .then(() => {
            Message.success(dt['message.update.success']);
            setVisible(false);
            callback();
          })
          .finally(() => setloading(false));
      });
    }
  };
  return (
    <>
      {readyOnly ? (
        <Button
          type="text"
          size="mini"
          icon={<IconUser />}
          onClick={() => setVisible(true)}
        >
          {triggerTitle}
        </Button>
      ) : (
        <Tooltip content={dt['sysadmin.table.operation.edit']}>
          <Button
            icon={<IconEdit />}
            type="text"
            size="small"
            onClick={() => setVisible(true)}
          />
        </Tooltip>
      )}
      <Modal
        title={dt['sysadmin.edit.form']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        okButtonProps={{ style: { display: readyOnly ? 'none' : null } }}
        confirmLoading={loading}
      >
        <Form layout={'vertical'} form={form}>
          <Item
            label={dt['sysadmin.form.fullname']}
            field={'fullName'}
            disabled={readyOnly}
          >
            <Input autoFocus placeholder={dt['from.input.placeholder']} />
          </Item>
          <Item
            label={dt['sysadmin.form.name']}
            field={'name'}
            rules={rules}
            disabled={readyOnly}
          >
            <Input placeholder={dt['from.input.placeholder']} />
          </Item>
          <Item
            label={dt['sysadmin.form.timezone']}
            field={'timezone'}
            disabled={readyOnly}
          >
            <Select
              placeholder={dt['from.select.placeholder']}
              allowClear
              options={timeList}
            />
          </Item>
          <Item
            label={dt['sysadmin.form.Owner']}
            field={'admin'}
            rules={[
              ...rules,
              {
                validator(value, callback) {
                  if (!regEmail.test(value) && !PhoneReg.test(value)) {
                    callback(dt['sysadmin.admin.error']);
                  }
                },
              },
            ]}
          >
            <Input placeholder={dt['sysadmin.from.admin']} />
          </Item>
          <Item
            label={dt['sysadmin.form.accountType']}
            field="type"
            rules={rules}
            disabled={readyOnly}
          >
            <Select
              allowClear
              placeholder={dt['from.select.placeholder']}
              options={[
                {
                  label: dt['sysadmin.form.accountType.normal'],
                  value: 'normal',
                },
                {
                  label: dt['sysadmin.form.accountType.trial'],
                  value: 'trial',
                },
              ]}
            />
          </Item>
          <Space size="large">
            {/* <Item
              triggerPropName="checked"
              label={dt['sysadmin.form.featureEnable']}
              field="featureEnable"
              rules={[{ required: true }]}
              disabled={readyOnly}
              initialValue={false}
            >
              <Switch />
            </Item> */}
            <Item
              triggerPropName="checked"
              label={dt['sysadmin.form.active']}
              field="active"
              rules={[{ required: true }]}
              disabled={readyOnly}
              initialValue={false}
            >
              <Switch />
            </Item>
          </Space>
        </Form>
      </Modal>
    </>
  );
}
