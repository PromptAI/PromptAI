import { updateUser } from '@/api/settings/users';
import useModalForm from '@/hooks/useModalForm';
import useRules, { useEmailRules, useMobileRules } from '@/hooks/useRules';
import i18n from '../locale';
import useLocale from '@/utils/useLocale';
import { Button, Form, Input, Modal, Space } from '@arco-design/web-react';
import { IconEdit, IconEmail, IconPhone } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import React, { PropsWithChildren, useMemo } from 'react';
import UpdatePwd from './UpdatePwd';

interface EditUserColumnProps {
  row: any;
  onSuccess: () => void;
}
const EditUserColumn = ({
  row,
  onSuccess,
  children,
}: PropsWithChildren<EditUserColumnProps>) => {
  const [visible, setVisible, form] = useModalForm(row);
  const onOk = async () => {
    const values = await form.validate();
    await updateUser({ id: row.id, ...values });
    onSuccess();
    setVisible(false);
  };
  const rules = useRules();
  const mobileRules = useMobileRules();
  const emailRules = useEmailRules();
  const mobileRule = useMemo(
    () =>
      isEmpty(row.mobile)
        ? mobileRules.filter((m) => !m.required)
        : mobileRules,
    [mobileRules, row.mobile]
  );
  const emailRule = useMemo(
    () =>
      isEmpty(row.email) ? emailRules.filter((e) => !e.required) : emailRules,
    [emailRules, row.email]
  );
  const t = useLocale(i18n);
  return (
    <div>
      <Button
        type="text"
        size="small"
        icon={<IconEdit />}
        onClick={() => setVisible(true)}
      >
        {children}
      </Button>
      <Modal
        title={t['user.update']}
        visible={visible}
        unmountOnExit
        onOk={onOk}
        onCancel={() => setVisible(false)}
        footer={(cancel, ok) => (
          <Space>
            {ok}
            <UpdatePwd key="update-pwd" userId={row.id} />
            {cancel}
          </Space>
        )}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label={t['user.name']} field="username" rules={rules}>
            <Input placeholder={t['user.name.placeholder']} />
          </Form.Item>
          <Form.Item label={t['user.mobile']} field="mobile" rules={mobileRule}>
            <Input
              prefix={<IconPhone />}
              placeholder={t['user.mobile.placeholder']}
            />
          </Form.Item>
          <Form.Item label={t['user.email']} field="email" rules={emailRule}>
            <Input
              prefix={<IconEmail />}
              placeholder={t['user.email.placeholder']}
            />
          </Form.Item>
          <Form.Item label={t['user.desc']} field="desc">
            <Input.TextArea autoSize />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default EditUserColumn;
