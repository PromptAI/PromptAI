import { updatePwd } from '@/api/auth';
import useRules from '@/hooks/useRules';
import { encrypt } from '@/utils/encrypt';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  FormInstance,
  Grid,
  Input,
  Message,
  Tabs,
} from '@arco-design/web-react';
import { IconSave } from '@arco-design/web-react/icon';
import React, { useEffect, useRef, useState } from 'react';
import { useHistory } from 'react-router';
import SecurityCode from '../components/SecurityCode';
import i18n from './locale';

interface UpdatePwdProps {
  defaultPwd?: string;
  sms?: string;
  email?: string;
}
const { Row, Col } = Grid;
const UpdatePwd = ({ defaultPwd, sms, email }: UpdatePwdProps) => {
  const t = useLocale(i18n);
  const history = useHistory();

  const formRef = useRef<FormInstance>();
  const rules = useRules();
  const [loading, setLoading] = useState(false);
  const [active, setActive] = useState<'pwd' | 'sms' | 'email'>('pwd');
  const handleSubmit = () => {
    formRef.current.validate().then(({ oldPass, newPass, code }) => {
      setLoading(true);
      updatePwd({
        oldPass: oldPass ? encrypt(oldPass) : undefined,
        newPass: encrypt(newPass),
        code,
        type: active,
      })
        .then(() => {
          Message.success(t['form.success']);
          formRef.current.resetFields();
          localStorage.removeItem('login_from_code');
          if (defaultPwd) {
            history.push('/projects');
          }
        })
        .finally(() => setLoading(false));
    });
  };
  useEffect(() => {
    formRef.current.setFieldsValue({ oldPass: defaultPwd });
  }, [defaultPwd]);
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
    <Row>
      <Col span={20}>
        <Tabs
          destroyOnHide
          activeTab={active}
          onChange={(key) => setActive(key as any)}
        >
          <Tabs.TabPane title={t[`form.title`]} key="pwd">
            <Form
              ref={formRef}
              title={t['form.title']}
              layout="vertical"
              style={{ maxWidth: 512 }}
            >
              <Form.Item
                label={t['form.old.password']}
                field="oldPass"
                rules={rules}
              >
                <Input.Password
                  placeholder={t['form.old.password.password.placeholder']}
                />
              </Form.Item>
              <Form.Item
                label={t['form.new.password']}
                field="newPass"
                rules={rules}
                onChange={handleChangeNewPass}
              >
                <Input.Password
                  placeholder={t['form.new.password.password.placeholder']}
                />
              </Form.Item>
              <Form.Item
                label={t['form.confirm.password']}
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
                  placeholder={t['form.confirm.password.placeholder']}
                />
              </Form.Item>
              <Form.Item>
                <Button
                  loading={loading}
                  type="primary"
                  icon={<IconSave />}
                  onClick={handleSubmit}
                >
                  {t['form.submit']}
                </Button>
              </Form.Item>
            </Form>
          </Tabs.TabPane>
          <Tabs.TabPane title={t[`form.sms`]} key="sms">
            <Form
              ref={formRef}
              title={t[`form.sms`]}
              layout="vertical"
              style={{ maxWidth: 512 }}
            >
              <Form.Item label={t[`form.sms.code`]} field="code" rules={rules}>
                <SecurityCode username={sms} type="resetPwd" use="sms" />
              </Form.Item>
              <Form.Item
                label={t['form.new.password']}
                field="newPass"
                rules={rules}
                onChange={handleChangeNewPass}
              >
                <Input.Password
                  placeholder={t['form.new.password.password.placeholder']}
                />
              </Form.Item>
              <Form.Item
                label={t['form.confirm.password']}
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
                  placeholder={t['form.confirm.password.placeholder']}
                />
              </Form.Item>
              <Form.Item>
                <Button
                  loading={loading}
                  type="primary"
                  icon={<IconSave />}
                  onClick={handleSubmit}
                >
                  {t['form.submit']}
                </Button>
              </Form.Item>
            </Form>
          </Tabs.TabPane>
          <Tabs.TabPane title={t[`form.email`]} key="email">
            <Form
              ref={formRef}
              title={t[`form.email`]}
              layout="vertical"
              style={{ maxWidth: 512 }}
            >
              <Form.Item
                label={t[`form.email.code`]}
                field="code"
                rules={rules}
              >
                <SecurityCode username={email} type="resetPwd" use="email" />
              </Form.Item>
              <Form.Item
                label={t['form.new.password']}
                field="newPass"
                rules={rules}
                onChange={handleChangeNewPass}
              >
                <Input.Password
                  placeholder={t['form.new.password.password.placeholder']}
                />
              </Form.Item>
              <Form.Item
                label={t['form.confirm.password']}
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
                  placeholder={t['form.confirm.password.placeholder']}
                />
              </Form.Item>
              <Form.Item>
                <Button
                  loading={loading}
                  type="primary"
                  icon={<IconSave />}
                  onClick={handleSubmit}
                >
                  {t['form.submit']}
                </Button>
              </Form.Item>
            </Form>
          </Tabs.TabPane>
        </Tabs>
      </Col>
    </Row>
  );
};

export default UpdatePwd;
