import {
  Form,
  Input,
  Button,
  Space,
  InputNumber,
  Modal,
} from '@arco-design/web-react';
import { IconPhone, IconEmail, IconUser } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import useLocale from '@/utils/useLocale';
import locale from './locale';
import styles from './index.module.less';
import nProgress from 'nprogress';
import { applying } from '@/api/auth';
import { regPhone, regEmail } from '@/utils/regex';
import { useHistory } from 'react-router-dom';

const Item = Form.Item;

export default function LoginForm() {
  const history = useHistory();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false); // 提交loading
  const t = useLocale(locale);

  const onSubmitClick = () => {
    form.validate().then((values) => {
      setLoading(true);
      applying({
        ...values,
        mobile: values.mobile ? values.mobile + '' : undefined,
        useScene: values.useScene ? values.useScene : '',
      })
        .then(() => {
          nProgress.start();

          Modal.success({
            title: t['applying.form.success'],
            content: t['applying.message'],
            maskClosable: false,
            onConfirm: async () => {
              history.push('/login');
            },
          });
          nProgress.done();
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
        <Item className="form-item" field="name">
          <Input
            prefix={<IconUser />}
            placeholder={t['applying.form.name.placeholder']}
          />
        </Item>
        <Item shouldUpdate noStyle>
          {(values) => (
            <Item
              className="form-item"
              field="email"
              rules={[
                {
                  validator: async (value, callback) => {
                    return new Promise((resolve) => {
                      if (!values.mobile && !value) {
                        callback(t['applying.form.email.required']);
                        resolve();
                        return;
                      }
                      if (!value) {
                        resolve();
                        return;
                      }
                      if (!regEmail.test(value)) {
                        callback(t['applying.form.email.errMsg']);
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
                placeholder={t['applying.form.email.placeholder']}
              />
            </Item>
          )}
        </Item>
        <Item shouldUpdate noStyle>
          {(values) => (
            <Item
              className="form-item"
              field="mobile"
              rules={[
                {
                  validator: async (value, callback) => {
                    return new Promise((resolve) => {
                      if (!values.email && !value) {
                        callback(t['applying.form.phone.required']);
                        resolve();
                        return;
                      }
                      if (!value) {
                        resolve();
                        return;
                      }
                      if (!regPhone.test(value)) {
                        callback(t['applying.form.phone.errMsg']);
                        resolve();
                      }
                      resolve();
                    });
                  },
                },
              ]}
            >
              <InputNumber
                prefix={<IconPhone />}
                placeholder={t['applying.form.phone.placeholder']}
                hideControl
              />
            </Item>
          )}
        </Item>
        <Item className="form-item" field="useScene">
          <Input.TextArea
            placeholder={t['applying.form.useScene.placeholder']}
          />
        </Item>
        <Space size={16} direction="vertical">
          <Button type="primary" onClick={onSubmitClick} long loading={loading}>
            {t['applying.submit']}
          </Button>
        </Space>
      </Form>
    </div>
  );
}
