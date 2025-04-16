import { updateUserPwd } from '@/api/settings/users';
import useModalForm from '@/hooks/useModalForm';
import i18n from '../locale';
import useLocale from '@/utils/useLocale';
import { Button, Form, Input, Modal } from '@arco-design/web-react';
import { IconSync } from '@arco-design/web-react/icon';
import React from 'react';

const UpdatePwd = ({ userId, ...rest }) => {
  const [visible, setVisible, form] = useModalForm();
  const onOk = async () => {
    const values = await form.validate();
    await updateUserPwd(values.password, userId);
    setVisible(false);
  };
  const t = useLocale(i18n);
  return (
    <div>
      <Button icon={<IconSync />} onClick={() => setVisible(true)} {...rest}>
        {t['user.password.reset']}
      </Button>
      <Modal
        title={t['user.password.reset']}
        visible={visible}
        unmountOnExit
        onOk={onOk}
        onCancel={() => setVisible(false)}
      >
        <Form layout="vertical" form={form}>
          <Form.Item
            label={t['user.password.reset']}
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
        </Form>
      </Modal>
    </div>
  );
};

export default UpdatePwd;
