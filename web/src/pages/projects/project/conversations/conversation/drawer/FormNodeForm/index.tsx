import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphForm } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import useRules from '@/hooks/useRules';
import { Form, Input } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle } from 'react';
import { SelectionProps } from '../../types';
import { updateFormNode } from '../operator';
import { ComponentHandle } from '../type';
import EnterBlurInput from '@/pages/projects/project/components/EnterBlurInput';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

const { Item } = Form;
const FormNodeForm = (
  {
    projectId,
    selection,
    onChangeEditSelection,
    onChange,
  }: SelectionProps<GraphForm>,
  ref: Ref<ComponentHandle>
) => {
  const t = useLocale(i18n);
  const formRef = useFormRef(selection.data);
  const rules = useRules();
  useImperativeHandle(
    ref,
    () => ({
      handle: async () => {
        await formRef.current.validate();
        const { id, parentId, relations, data } = selection;
        return updateFormNode({
          projectId,
          id,
          data,
          parentId,
          relations,
          callback: (node) => {
            onChange((vals) =>
              ObjectArrayHelper.update(vals, node, (v) => v.id === id)
            );
          },
        });
      },
    }),
    [formRef, onChange, projectId, selection]
  );
  const onValuesChange = (_, values) => {
    onChangeEditSelection({ ...selection, data: values });
  };
  return (
    <Form
      layout="vertical"
      ref={formRef}
      initialValues={selection.data}
      onValuesChange={onValuesChange}
    >
      <Item label={t['drawer.form.form.name']} field="name" rules={rules}>
        <EnterBlurInput
          autoFocus
          placeholder={t['drawer.form.form.name.des']}
        />
      </Item>
      <Item label={t['drawer.form.form.description']} field="description">
        <Input.TextArea
          placeholder={t['drawer.form.form.description.des']}
          autoSize
        />
      </Item>
    </Form>
  );
};

export default FormNodeForm;
