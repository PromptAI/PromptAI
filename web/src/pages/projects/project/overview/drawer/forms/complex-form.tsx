import { updateProject } from '@/api/projects';
import { GraphNode } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import useRules from '@/hooks/useRules';
import { useProjectContext } from '@/layout/project-layout/context';
import useLocale from '@/utils/useLocale';
import { Form, Radio, Select } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle, useMemo } from 'react';
import { SelectionProps } from '../../types';
import i18n from '../locale';
import { ComponentHandle } from '../type';
import ShowBranshAsButtonView from '../../../components/ShowBranshAsButtonView';

interface Complex extends GraphNode {
  data: {
    branchWelcome: string;
    showSubNodesByOptional: boolean;
  };
}
const ComplexForm = (
  { selection, graph }: SelectionProps<Complex>,
  ref: Ref<ComponentHandle>
) => {
  const defaultSelection = useMemo(
    () =>
      graph
        ?.filter((g) => g.type === 'conversation' && !g.data.hidden)
        .map((g) => g.id) || [],
    [graph]
  );
  const formRef = useFormRef({
    ...selection.data,
    showShbNodesAsOptionalIds: defaultSelection,
  });
  const rules = useRules();
  const t = useLocale(i18n);
  const { refresh, ...project } = useProjectContext();
  useImperativeHandle(ref, () => ({
    handle: async () => {
      const values = await formRef.current.validate();
      await updateProject({ ...project, ...values });
      refresh();
    },
  }));
  const flowOptions = useMemo(
    () =>
      graph
        ?.filter((g) => g.type === 'conversation')
        .map((g) => ({ label: g.data.name, value: g.id })) || [],
    [graph]
  );

  return (
    <Form layout="vertical" ref={formRef}>
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

      {/* <Form.Item
        label={t['complex.form.optional.number']}
        initialValue={3}
        field="showSubNodesCount"
        wrapperCol={{ span: 4 }}
        layout="inline"
      >
        <InputNumber
          precision={0}
          min={0}
          suffix={t['complex.form.optional.number.suffix']}
        />
      </Form.Item> */}
      <Form.Item shouldUpdate noStyle>
        {({ showSubNodesAsOptional, showShbNodesAsOptionalIds }) => (
          <ShowBranshAsButtonView
            mode={showSubNodesAsOptional}
            buttons={flowOptions}
            shows={showShbNodesAsOptionalIds}
          />
        )}
      </Form.Item>
    </Form>
  );
};

export default ComplexForm;
