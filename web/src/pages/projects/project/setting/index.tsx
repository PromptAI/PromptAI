import { updateProject } from '@/api/projects';
import useRules from '@/hooks/useRules';
import { useProjectContext } from '@/layout/project-layout/context';
import useLocale, { useDefaultLocale, useLocaleLang } from '@/utils/useLocale';
import {
  Button,
  Card,
  Form,
  Input,
  Message,
  Radio,
  Select,
} from '@arco-design/web-react';
import React, { useMemo, useState } from 'react';
import CropImageUpload from '../components/CropImageUpload';
import i18n from './locale';
import ShowBranshAsButtonView from '../components/ShowBranshAsButtonView';
import { useRequest } from 'ahooks';
import { listConversations, listFaqs } from '@/api/components';
import useFormRef from '@/hooks/useFormRef';

const { Item: FormItem } = Form;

const fetchOptions = async (projectId: string) => {
  const flows = await listConversations(projectId);
  // Temporarily cancel faq
  // const faqs = await listFaqs(projectId);
  // return [...faqs, ...flows];
  return [...flows];
};

export default function () {
  const t = useLocale(i18n);
  const rules = useRules();
  const dt = useDefaultLocale();
  const lang = useLocaleLang();

  const { refresh, ...data } = useProjectContext();

  const [loading, setLoading] = useState(false);
  const { data: graph = [] } = useRequest(() => fetchOptions(data.id), {
    refreshDeps: [data.id],
  });
  const defaultSelection = useMemo(
    () => graph?.filter((g) => !g.data.hidden).map((g) => g.id) || [],
    [graph]
  );
  const flowOptions = useMemo(
    () => graph?.map((g) => ({ label: g.data.name, value: g.id })) || [],
    [graph]
  );
  const formRef = useFormRef({
    ...data,
    showShbNodesAsOptionalIds: defaultSelection,
  });

  const onOk = () => {
    setLoading(true);
    formRef.current
      .validate()
      .then(async (values) => {
        updateProject({
          ...data,
          ...values,
          id: data.id,
          welcome: values.welcome || t['project.from.welcome.placeholder'],
          fallback: values?.fallback || t['project.form.Unknown.placeholder'],
          locale: lang.startsWith('en') ? 'en' : 'zh',
        })
          .then(() => {
            Message.success(dt['message.update.success']);
            refresh();
          })
          .catch(() => setLoading(false));
      })
      .catch(() => null);
  };

  return (
    <Card
      style={{ width: 600 }}
      title={t['setting.title']}
      extra={
        <Button type="primary" loading={loading} onClick={onOk}>
          {t['project.update.title']}
        </Button>
      }
    >
      <Form ref={formRef} layout="vertical">
        <FormItem label={t['project.form.name']} field="name" rules={rules}>
          <Input autoFocus placeholder="" />
        </FormItem>
        <FormItem label={t['project.form.welcome']} field="welcome">
          <Input.TextArea
            style={{ minHeight: 64 }}
            placeholder={t['project.from.welcome.placeholder']}
          />
        </FormItem>
        <FormItem label={t['project.form.Unknown']} field="fallback">
          <Input.TextArea
            style={{ minHeight: 64 }}
            placeholder={t['project.form.Unknown.placeholder']}
          />
        </FormItem>
        <Form.Item
          label={t['complex.form.optional']}
          rules={rules}
          initialValue={true}
          field="showSubNodesAsOptional"
        >
          <Radio.Group>
            <Radio value="none">{t['complex.form.optional.none']}</Radio>
            <Radio value="all">{t['complex.form.optional.all']}</Radio>
            <Radio value="custom">{t['complex.form.optional.custom']}</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item shouldUpdate noStyle>
          {({ showSubNodesAsOptional }) =>
            showSubNodesAsOptional === 'custom' && (
              <Form.Item
                rules={rules}
                initialValue={true}
                field="showShbNodesAsOptionalIds"
              >
                <Select
                  options={flowOptions}
                  placeholder={t['complex.form.optional.custom.selection']}
                  mode="multiple"
                  allowClear
                />
              </Form.Item>
            )
          }
        </Form.Item>
        <Form.Item shouldUpdate noStyle>
          {({ showSubNodesAsOptional, showShbNodesAsOptionalIds }) => (
            <ShowBranshAsButtonView
              mode={showSubNodesAsOptional}
              buttons={flowOptions}
              shows={showShbNodesAsOptionalIds}
            />
          )}
        </Form.Item>
        <FormItem label={t['project.form.image']} field="image">
          <CropImageUpload cropSize={{ width: 420, height: 260 }} />
        </FormItem>
        <FormItem label={t['project.form.description']} field="description">
          <Input.TextArea autoSize placeholder="" />
        </FormItem>
      </Form>
    </Card>
  );
}
