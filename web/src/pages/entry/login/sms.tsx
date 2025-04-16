import {
  Form,
  Button,
  Space,
  Input,
  Divider,
  Typography,
} from '@arco-design/web-react';
import { FormInstance } from '@arco-design/web-react/es/Form';
import { IconUser } from '@arco-design/web-react/icon';
import React, { useRef, useState } from 'react';
import useLocale from '@/utils/useLocale';
import locale from './locale';
import styles from './index.module.less';
import { authLogin } from '@/api/auth';
import SecurityCode from '@/pages/components/SecurityCode';
import useRules from '@/hooks/useRules';
import { regPhone, regEmail } from '@/utils/regex';
import { useHistory } from 'react-router-dom';
import afterLogin from './afterLogin';
import LoginComponent from './google/LoginComponent';

export default function SmsForm() {
  const history = useHistory();
  const formRef = useRef<FormInstance>();
  const [loading, setLoading] = useState(false);

  const t = useLocale(locale);

  function onSubmitClick() {
    formRef.current
      .validate()
      .then((values) => {
        setLoading(true);
        const type = regPhone.test(values.username) ? 'sms' : 'email';
        authLogin({ ...values, username: values.username + '', type })
          .then((data) => {
            afterLogin(data, history, values, t);
          })
          .finally(() => setLoading(false));
      })
      .catch(() => null);
  }

  const goRegister = () => history.push('/register');
  const rules = useRules();

  return (
    <>
      <Form
        className={styles['login-form']}
        layout="vertical"
        ref={formRef}
        initialValues={{}}
      >
        <Form.Item
          className="form-item"
          field="username"
          rules={[
            {
              validator: async (value, callback) => {
                return new Promise((resolve) => {
                  if (!regPhone.test(value) && !regEmail.test(value)) {
                    callback(t['register.form.phone.and.email.errMsg']);
                    resolve();
                  }
                  resolve();
                });
              },
            },
            ...rules,
          ]}
        >
          <Input
            prefix={<IconUser />}
            placeholder={t['login.form.userName.mobile.placeholder']}
          />
        </Form.Item>
        <Form.Item noStyle shouldUpdate>
          {(values) => (
            <Form.Item className="form-item" field={'code'} rules={rules}>
              <SecurityCode
                disabled={
                  !regPhone.test(values.username) &&
                  !regEmail.test(values.username)
                }
                use={regPhone.test(values.username) ? 'sms' : 'email'}
                username={values.username}
                type="login"
              />
            </Form.Item>
          )}
        </Form.Item>
        <div style={{ height: 37 }}></div>
        <Space size={8} direction="vertical">
          <Button type="primary" long onClick={onSubmitClick} loading={loading}>
            {t['login.form.login']}
          </Button>
          <Divider style={{ margin: 0 }}>{t['login.form.or']}</Divider>
          <LoginComponent />
          <div style={{ marginTop: 16 }}>
            <Typography.Text>
              {t['login.form.register']}
              <Typography.Text
                type="primary"
                style={{
                  marginLeft: 16,
                  textDecorationLine: 'underline',
                  cursor: 'pointer',
                }}
                onClick={goRegister}
              >
                {t['login.form.register.createAccount']}
              </Typography.Text>
            </Typography.Text>
          </div>
        </Space>
      </Form>
    </>
  );
}
