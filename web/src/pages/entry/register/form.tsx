import { Form, Input, Button, Notification } from '@arco-design/web-react';
import { IconEmail } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import useLocale from '@/utils/useLocale';
import locale from './locale';
import styles from './index.module.less';
import { registerApply } from '@/api/auth';
import { regEmail } from '@/utils/regex';

const Item = Form.Item;

export default function RegisterForm() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false); // 提交loading
  const t = useLocale(locale);

  const onSubmitClick = () => {
    form.validate().then((values) => {
      setLoading(true);
      registerApply({ type: 'active_email', ...values })
        .then(() => {
          Notification.info({
            closable: false,
            title: t['register.form.submit.notification.title'],
            content: t['register.form.submit.notification.content'],
          });
          form.clearFields();
        })
        .finally(() => setLoading(false));
    });
  };

  return (
    <div className={styles['register-form-wrapper']}>
      <Form
        className={styles['register-form']}
        layout="vertical"
        form={form}
        initialValues={{}}
      >
        <Item
          className="form-item"
          field="email"
          rules={[
            {
              validator: async (value, callback) => {
                return new Promise((resolve) => {
                  if (!regEmail.test(value)) {
                    callback(t['register.form.email.errMsg']);
                    resolve();
                  }
                  resolve();
                });
              },
            },
          ]}
        >
          <Input
            prefix={<IconEmail />}
            placeholder={t['register.form.email.placeholder']}
          />
        </Item>
        <Button
          style={{ marginTop: 16 }}
          type="primary"
          onClick={onSubmitClick}
          long
          loading={loading}
        >
          {t['register.submit']}
        </Button>
      </Form>
    </div>
  );
}
