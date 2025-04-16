import {
  Form,
  Input,
  Checkbox,
  Button,
  Space,
  Typography,
  Divider,
} from '@arco-design/web-react';
import { FormInstance } from '@arco-design/web-react/es/Form';
import { IconLock, IconUser } from '@arco-design/web-react/icon';
import React, { useEffect, useRef, useState } from 'react';
import { decode, encode } from 'js-base64';
import useStorage from '@/utils/useStorage';
import useLocale from '@/utils/useLocale';
import locale from './locale';
import styles from './index.module.less';
import { encrypt } from '@/utils/encrypt';
import { authLogin } from '@/api/auth';
import { useHistory } from 'react-router-dom';
import afterLogin from './afterLogin';
import LoginComponent from './google/LoginComponent';

const isEnEnv = process.env.REACT_APP_LANG === 'en-US';

export default function LoginForm() {
  const history = useHistory();
  const formRef = useRef<FormInstance>();
  const [loading, setLoading] = useState(false);
  const [loginParams, setLoginParams, removeLoginParams] =
    useStorage('loginParams');

  const t = useLocale(locale);

  const [rememberPassword, setRememberPassword] = useState(!!loginParams);

  const login = async (params) => {
    setLoading(true);
    const data = { ...params, password: encrypt(params.password) };
    try {
      const response = await authLogin(data);
      // 记住密码
      rememberPassword
        ? setLoginParams(encode(JSON.stringify(params)))
        : removeLoginParams();

      await afterLogin(response, history, params, t);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  function onSubmitClick() {
    formRef.current
      .validate()
      .then(({ code, ...other }) => {
        login({ ...other, ...code, type: 'passcode' });
      })
      .catch(() => null);
  }

  // 读取 localStorage，设置初始值
  useEffect(() => {
    if (formRef.current && !!loginParams) {
      try {
        const parseParams = JSON.parse(decode(loginParams));
        formRef.current.setFieldsValue(parseParams);
      } catch (error) {}
    }
  }, [loginParams]);

  const goRegister = () => history.push('/register');
  return (
    <>
      <Form
        className={styles['login-form']}
        layout="vertical"
        ref={formRef}
        initialValues={{}}
      >
        <Form.Item
          field="username"
          rules={[{ required: true, message: t['login.form.userName.errMsg'] }]}
        >
          <Input
            prefix={<IconUser />}
            placeholder={t['login.form.userName.placeholder']}
            onPressEnter={onSubmitClick}
          />
        </Form.Item>
        <Form.Item
          field="password"
          rules={[{ required: true, message: t['login.form.password.errMsg'] }]}
        >
          <Input.Password
            prefix={<IconLock />}
            placeholder={t['login.form.password.placeholder']}
            onPressEnter={onSubmitClick}
          />
        </Form.Item>
        <Space size={8} direction="vertical">
          <div className={styles['login-form-password-actions']}>
            <Checkbox checked={rememberPassword} onChange={setRememberPassword}>
              {t['login.form.rememberPassword']}
            </Checkbox>
            <Typography.Text
              type="primary"
              style={{
                textDecorationLine: 'underline',
                cursor: 'pointer',
              }}
              onClick={() => history.push('/forgot')}
            >
              {t['login.form.forgot']}
            </Typography.Text>
          </div>
          <Button type="primary" long onClick={onSubmitClick} loading={loading}>
            {t['login.form.login']}
          </Button>

          {isEnEnv && (
            <>
              <Divider style={{ margin: 0 }}>{t['login.form.or']}</Divider>
            </>
          )}
          {isEnEnv && <LoginComponent />}
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
