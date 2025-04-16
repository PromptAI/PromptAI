import { GraphNode } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import useRules from '@/hooks/useRules';
import { Form, Input, Radio, Select } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle, useMemo } from 'react';
import { SelectionProps } from '../../types';
import { ComponentHandle } from '../type';
import { useProjectContext } from '@/layout/project-layout/context';
import { updateProject } from '@/api/projects';
import CropImageUpload from '../../../components/CropImageUpload';
import useLocale from '@/utils/useLocale';
import i18n from '../locale';
import ShowBranshAsButtonView from '../../../components/ShowBranshAsButtonView';

interface GraphProject extends GraphNode {
  data: {
    name: string;
    local: string;
    description?: string;
    welcome?: string;
    image?: string;
  };
}
const options = [
  { label: '中文', value: 'zh' },
  { label: 'English', value: 'en' },
];
const ProjectForm = (
  { selection, graph }: SelectionProps<GraphProject>,
  ref: Ref<ComponentHandle>
) => {
  const defaultSelection = useMemo(
    () =>
      graph
        ?.filter(
          (g) => ['conversation', 'sample'].includes(g.type) && !g.data.hidden
        )
        .map((g) => g.id) || [],
    [graph]
  );
  const formRef = useFormRef({
    ...selection.data,
    showShbNodesAsOptionalIds: defaultSelection,
  });
  const rules = useRules();
  const { refresh, ...project } = useProjectContext();
  useImperativeHandle(ref, () => ({
    handle: async () => {
      const values = await formRef.current.validate();
      await updateProject({ ...project, ...values });
      refresh();
    },
  }));
  const t = useLocale(i18n);
  const flowOptions = useMemo(
    () =>
      graph
        ?.filter((g) => ['conversation', 'sample'].includes(g.type))
        .map((g) => ({ label: g.data.name, value: g.id })) || [],
    [graph]
  );
  return (
    <Form layout="vertical" ref={formRef} initialValues={selection.data}>
      <Form.Item label={t['project.form.name']} field="name" rules={rules}>
        <Input placeholder="" />
      </Form.Item>
      <Form.Item
        label={t['project.form.lang']}
        required
        field="locale"
        rules={rules}
      >
        <Select options={options} />
      </Form.Item>
      <Form.Item label={t['project.form.welcome']} field="welcome">
        <Input.TextArea
          autoSize={{ minRows: 2, maxRows: 4 }}
          placeholder={t['project.form.welcome.placeholder']}
        />
      </Form.Item>
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
      <Form.Item label={t['project.form.desc']} field="description">
        <Input.TextArea autoSize placeholder="" />
      </Form.Item>
      <Form.Item label={t['project.form.image']} field="image">
        <CropImageUpload cropSize={{ width: 420, height: 260 }} />
      </Form.Item>
    </Form>
  );
};

export default ProjectForm;
