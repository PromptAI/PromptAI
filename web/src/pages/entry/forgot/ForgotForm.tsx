import React, { useRef, useState } from 'react';
import {
  Button,
  Form,
  FormInstance,
  Input,
  Message,
  Typography,
} from '@arco-design/web-react';
import { useHistory } from 'react-router';
import { checkCode, resetForgotPwd } from '@/api/resePwd';
import useStep from '@/hooks/useStep';
import { regEmail } from '@/utils/regex';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { IconEmail, IconLock } from '@arco-design/web-react/icon';
import SecurityCode from '@/pages/components/SecurityCode';
import useRules from '@/hooks/useRules';

const ForgotForm = () => {
  const t = useLocale(i18n);
  const history = useHistory();
  const formRef = useRef<FormInstance>();

  const rules = useRules();

  const [cache, setCache] = useState({ email: '', code: '' });
  const [submiting, setSubmiting] = useState(false);
  const onSubmit = () => {
    formRef.current.validate().then((values) => {
      const params = { ...cache, ...values };
      delete params.confirmPass;
      setSubmiting(true);
      resetForgotPwd(params)
        .then(() => {
          Message.success('success');
          history.push('/login');
        })
        .finally(() => setSubmiting(false));
    });
  };

  const { step, next, pre, nextLoading } = useStep({
    completeStep: 1,
    nextCondition: async (s) => {
      if (s === 0) {
        try {
          const { email, code } = await formRef.current.validate([
            'email',
            'code',
          ]);
          await checkCode({ token: email, code });
          setCache({ email, code });
          return true;
        } catch (error) {
          return false;
        }
      }
      return true;
    },
  });
  const handleChangeNewPass = () => {
    const newPass = formRef.current.getFieldValue('newPass');
    const confirmPass = formRef.current.getFieldValue('confirmPass');
    if (newPass === confirmPass) {
      formRef.current.setFields({
        confirmPass: { value: confirmPass, error: null },
      });
    } else {
      formRef.current.setFields({
        confirmPass: {
          value: confirmPass,
          error: {
            value: confirmPass,
            message: t['form.confirm.password.error'],
          },
        },
      });
    }
  };
  return (
    <>
      <Form ref={formRef} layout="vertical">
        {step === 0 && (
          <>
            <Form.Item
              field="email"
              rules={[
                {
                  validator: async (value, callback) => {
                    return new Promise((resolve) => {
                      if (!regEmail.test(value)) {
                        callback(t['form.email.errMsg']);
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
                placeholder={t['form.email.placeholder']}
              />
            </Form.Item>
            <Form.Item shouldUpdate noStyle>
              {(values) => (
                <Form.Item field="code" rules={rules}>
                  <SecurityCode
                    disabled={!regEmail.test(values.email)}
                    username={values.email}
                    use="email"
                    type="forgot"
                  />
                </Form.Item>
              )}
            </Form.Item>
            <div className="w-full mt-2 flex gap-2">
              <Button type="primary" loading={nextLoading} long onClick={next}>
                {t['forgot.step.next']}
              </Button>
            </div>
          </>
        )}
        {step === 1 && (
          <>
            <Form.Item
              field="newPass"
              rules={rules}
              onChange={handleChangeNewPass}
            >
              <Input.Password
                prefix={<IconLock />}
                placeholder={t['form.new.password']}
              />
            </Form.Item>
            <Form.Item
              field="confirmPass"
              rules={[
                ...rules,
                {
                  validator: (value, callback) => {
                    if (formRef.current.getFieldValue('newPass') !== value) {
                      callback(t['form.confirm.password.error']);
                    }
                  },
                },
              ]}
            >
              <Input.Password
                prefix={<IconLock />}
                placeholder={t['form.confirm.password']}
              />
            </Form.Item>
            <div className="w-full mt-2 flex gap-2">
              <Button long onClick={pre}>
                {t['forgot.step.previou']}
              </Button>
              <Button
                long
                type="primary"
                loading={submiting}
                onClick={onSubmit}
              >
                {t['forgot.step.submit']}
              </Button>
            </div>
          </>
        )}
      </Form>
      <div style={{ marginTop: 16 }}>
        <Typography.Text>
          {t['forgot.redirect.title']}
          <Typography.Text
            type="primary"
            style={{
              marginLeft: 16,
              textDecorationLine: 'underline',
              cursor: 'pointer',
            }}
            onClick={() => history.push('/login')}
          >
            {t['forgot.redirect.subtitle']}
          </Typography.Text>
        </Typography.Text>
      </div>
    </>
  );
};

export default ForgotForm;
