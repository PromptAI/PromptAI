import { createProject } from '@/api/projects';
import useModalForm from '@/hooks/useModalForm';
import useRules from '@/hooks/useRules';
import useLocale, { useLocaleLang } from '@/utils/useLocale';
import {
  Button,
  Form,
  Input,
  Message,
  Modal,
  Select,
} from '@arco-design/web-react';
import { IconPlus } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import { useHistory } from 'react-router';
import i18n from './locale';
import CropImageUpload from './project/components/CropImageUpload';
import useFeatureEnable from '@/hooks/useFeatureEnable';

const { Item: FormItem } = Form;
const CreateProject = () => {
  const t = useLocale(i18n);
  const dt = useLocale();
  const lang = useLocaleLang();
  const history = useHistory();
  const [visible, setVisible, form] = useModalForm();
  const rules = useRules();
  const [loading, setLoading] = useState(false);

  const onOk = async () => {
    let values = await form.validate();
    setLoading(true);
    try {
      values = { ...values, locale: lang.startsWith('en') ? 'en' : 'zh' };
      const { id } = await createProject(values);
      Message.success(dt['message.create.success']);
      history.push(`/projects/${id}/tool/setting`);
    } catch (error) {
      setLoading(false);
    }
  };

  return (
    <div>
      <Button
        type="primary"
        icon={<IconPlus />}
        onClick={() => setVisible(true)}
      >
        {t['project.create']}
      </Button>
      <Modal
        title={t['project.create.title']}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={onOk}
        confirmLoading={loading}
        unmountOnExit
        style={{ width: 550,paddingBottom: 0 }}
      >
        <Form form={form} layout="vertical">
          <FormItem label={t['project.form.name']} field="name" rules={rules}>
            <Input placeholder="" />
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
          <FormItem label={t['project.form.image']} field="image">
            <CropImageUpload cropSize={{ width: 480, height: 270 }} />
          </FormItem>
        </Form>
      </Modal>
    </div>
  );
};

export default CreateProject;
