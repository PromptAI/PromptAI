import { createUser } from '@/api/settings/users';
import useModalForm from '@/hooks/useModalForm';
import useRules, { useEmailRules, useMobileRules } from '@/hooks/useRules';
import i18n from '../locale';
import useLocale from '@/utils/useLocale';
import { Button, Form, Input, Link, Modal } from '@arco-design/web-react';
import { IconEmail, IconPhone, IconPlus } from '@arco-design/web-react/icon';
import React, { PropsWithChildren, useState } from 'react';

interface CreateUserProps {
  onSuccess: () => void;
}
const CreateUser = ({
  onSuccess,
  children,
}: PropsWithChildren<CreateUserProps>) => {
  const [visible, setVisible, form] = useModalForm();
  const onOk = async () => {
    const values = await form.validate();
    await createUser({ ...values });
    onSuccess();
    setVisible(false);
  };
  const [type, setType] = useState<'mobile' | 'email'>('mobile');
  const rules = useRules();
  const mobileRules = useMobileRules();
  const emailRules = useEmailRules();
  const t = useLocale(i18n);
  return (
    <div>
      <Button
        type="primary"
        icon={<IconPlus />}
        onClick={() => setVisible(true)}
      >
        {children}
      </Button>
      <Modal
        title={t['user.create']}
        visible={visible}
        unmountOnExit
        onOk={onOk}
        onCancel={() => setVisible(false)}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label={t['user.name']} field="username" rules={rules}>
            <Input placeholder={t['user.name.placeholder']} />
          </Form.Item>
          {type === 'mobile' && (
            <Form.Item
              label={t['user.mobile']}
              field="mobile"
              rules={mobileRules}
            >
              <Input
                prefix={<IconPhone />}
                placeholder={t['user.mobile.placeholder']}
                suffix={
                  <Link
                    style={{ marginRight: 8 }}
                    onClick={() => {
                      setType('email');
                      form.clearFields('mobile');
                    }}
                  >
                    {t['user.type.email']}
                  </Link>
                }
              />
            </Form.Item>
          )}
          {type === 'email' && (
            <Form.Item label={t['user.email']} field="email" rules={emailRules}>
              <Input
                prefix={<IconEmail />}
                placeholder={t['user.email.placeholder']}
                suffix={
                  <Link
                    style={{ marginRight: 8 }}
                    onClick={() => {
                      setType('mobile');
                      form.clearFields('email');
                    }}
                  >
                    {t['user.type.mobile']}
                  </Link>
                }
              />
            </Form.Item>
          )}
          <Form.Item
            label={t['user.password']}
            field="password"
            required
            rules={[
              {
                validator(value, cb) {
                  if (!value)
                    return Promise.reject(t['user.password.required']);
                  if (value.length < 10) return cb(t['user.password.rule']);
                  if (
                    !/(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9])/.test(value)
                  )
                    return cb(t['user.password.rule']);
                  return cb();
                },
              },
            ]}
          >
            <Input.Password placeholder={t['user.password.placeholder']} />
          </Form.Item>
          <Form.Item
            label={t['user.password.confirm']}
            field="confirm"
            rules={[
              { required: true },
              {
                validator(value, callback) {
                  if (form.getFieldValue('password') !== value) {
                    return callback(t['user.password.confirm.rule']);
                  }
                  return callback();
                },
              },
            ]}
          >
            <Input.Password placeholder={t['user.password.placeholder']} />
          </Form.Item>
          <Form.Item label={t['user.password.desc']} field="desc">
            <Input.TextArea autoSize />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default CreateUser;
