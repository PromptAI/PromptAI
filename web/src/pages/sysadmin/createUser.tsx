import { addAccount } from '@/api/projects';
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
} from '@arco-design/web-react';
import { IconUserAdd } from '@arco-design/web-react/icon';
import React, { useState } from 'react';

const PhoneReg = new RegExp('^[1][3,5,7,8][0-9]{9}$');

const { Item } = Form;

export default function CreateUser({ callback }) {
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();

  const dt = useDefaultLocale();

  const [loading, setloading] = useState(false);
  const onOk = () => {
    form.validate().then((res) => {
      setloading(true);
      const { admins, ...rest } = res;
      let params = {};
      if (admins) {
        const ArrayAdmis = (admins && admins.split(',')) || null;
        params = { ...rest, admins: ArrayAdmis };
      } else {
        params = { ...res };
      }

      addAccount({ ...params })
        .then(() => {
          Message.success(dt['message.create.success']);
          setVisible(false);
          callback();
        })
        .finally(() => setloading(false));
    });
  };
  return (
    <>
      <Button
        icon={<IconUserAdd />}
        type="primary"
        onClick={() => setVisible(true)}
      >
        {dt['sysadmin.create.form']}
      </Button>
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        title={dt['sysadmin.create.form']}
        confirmLoading={loading}
      >
        <Form
          layout={'vertical'}
          form={form}
          labelCol={{ span: 7 }}
          wrapperCol={{ span: 17 }}
        >
          <Item label={dt['sysadmin.form.fullname']} field={'fullName'}>
            <Input autoFocus placeholder={dt['from.input.placeholder']} />
          </Item>
          <Item label={dt['sysadmin.form.name']} field={'name'} rules={rules}>
            <Input placeholder={dt['from.input.placeholder']} />
          </Item>
          <Item
            label={dt['sysadmin.form.timezone']}
            field={'timezone'}
            rules={rules}
          >
            {/* <Input placeholder={dt['from.input.placeholder']} /> */}
            <Select
              placeholder={dt['from.select.placeholder']}
              allowClear
              options={[
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
              ]}
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
            label={dt['sysadmin.form.admins']}
            field={'admins'}
            rules={[
              {
                validator(value, callback) {
                  if (value) {
                    const validator = value.split(',').some((c) => {
                      return !regEmail.test(c) && !PhoneReg.test(c);
                    });
                    if (validator) {
                      callback(dt['sysadmin.admin.error']);
                    }
                  }
                },
              },
            ]}
          >
            <Input placeholder={dt['sysadmin.form.admins.placeholder']} />
          </Item>
          <Item
            label={dt['sysadmin.form.accountType']}
            field="type"
            rules={rules}
          >
            <Select
              placeholder={dt['from.select.placeholder']}
              allowClear
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
        </Form>
      </Modal>
    </>
  );
}
