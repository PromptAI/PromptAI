import { cloneProject } from '@/api/lib';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import { Form, Input, Message, Modal, Select } from '@arco-design/web-react';
import { IconCloudDownload } from '@arco-design/web-react/icon';
import React from 'react';
import i18n from './locale/index';
import { useHistory } from 'react-router';
import CropImageUpload from '../projects/project/components/CropImageUpload';

const { Item: FormItem } = Form;
const ImportCard = ({
  initialValues,
  trigger,
}: {
  initialValues: any;
  trigger: React.ReactElement;
}) => {
  const history = useHistory();
  const t = useLocale(i18n);
  const [visible, setVisible, form] = useModalForm({
    ...initialValues,
    name: initialValues.name.endsWith(`（${t['copy']}）`)
      ? initialValues.name
      : `${initialValues.name} （${t['copy']}）`,
  });

  const rules = useRules();
  const onOk = async () => {
    const values = await form.validate();
    try {
      const result = await cloneProject({
        ...values,
        templateProjectId: initialValues.id,
        welcome: values.welcome || t['project.from.welcome.placeholder'],
        fallback: values?.fallback || t['project.form.Unknown.placeholder'],
      });
      Message.success(t['lib.import.success']);
      setVisible(false);
      history.replace(`/projects/${result.id}/overview`);
    } catch (e) {}
  };

  return (
    <div>
      {React.cloneElement(trigger, { onClick: () => setVisible(true) })}
      <Modal
        title={`${t['lib.import']} ${initialValues.name} ${t['lib.project']}`}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        okText={t['lib.import']}
        okButtonProps={{
          icon: <IconCloudDownload />,
        }}
        style={{ width: 580 }}
      >
        <Form form={form} layout="vertical">
          <FormItem label={t['lib.form.name']} field="name" rules={rules}>
            <Input placeholder="" />
          </FormItem>
          <FormItem
            label={t['lib.form.locale']}
            required
            field="locale"
            rules={rules}
          >
            <Select
              options={[
                { label: '中文', value: 'zh' },
                { label: 'English', value: 'en' },
              ]}
            />
          </FormItem>
          <FormItem label={t['lib.form.description']} field="description">
            <Input.TextArea autoSize placeholder="" />
          </FormItem>
          <FormItem label={t['project.form.welcome']} field="welcome">
            <Input.TextArea
              autoSize={{ minRows: 2, maxRows: 4 }}
              placeholder={t['project.from.welcome.placeholder']}
            />
          </FormItem>
          <FormItem label={t['project.form.Unknown']} field="fallback">
            <Input.TextArea
              autoSize={{ minRows: 2, maxRows: 4 }}
              placeholder={t['project.form.Unknown.placeholder']}
            />
          </FormItem>
          <FormItem label={t['lib.form.image']} field="image">
            <CropImageUpload cropSize={{ width: 420, height: 260 }} />
          </FormItem>
        </Form>
      </Modal>
    </div>
  );
};

export default ImportCard;
