import { GraphBreak } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import { Form } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle } from 'react';
import { SelectionProps } from '../../types';
import { updateBreak } from '../operator';
import ColorInput from './ColorInput';
import produce from 'immer';
import { ComponentHandle } from '../type';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import EnterBlurInput from '@/pages/projects/project/components/EnterBlurInput';

const { Item } = Form;

export default (
  {
    projectId,
    selection,
    onChange,
    onChangeEditSelection,
  }: SelectionProps<GraphBreak>,
  ref: Ref<ComponentHandle>
) => {
  const t = useLocale(i18n);
  const formRef = useFormRef(selection.data);
  const onValuesChange = (_, values) => {
    onChangeEditSelection({
      ...selection,
      data: values,
    });
  };
  useImperativeHandle(
    ref,
    () => ({
      handle: async () => {
        await formRef.current.validate();
        const { id, relations, parentId, data } = selection;
        return updateBreak({
          projectId,
          id,
          parentId,
          relations,
          data,
          callback: (node) => {
            onChange(
              produce((nodes) => {
                const breakNodeIndex = nodes.findIndex((o) => o.id === node.id);
                if (breakNodeIndex > -1) {
                  nodes[breakNodeIndex] = node;
                }

                const conditionIndex = nodes.findIndex(
                  (o) =>
                    o.type === 'condition' && o.id === node.data.conditionId
                );

                if (conditionIndex > -1) {
                  nodes[conditionIndex].data.name = node.data.name;
                  nodes[conditionIndex].data.color = node.data.color;
                }
              })
            );
          },
        });
      },
    }),
    [formRef, onChange, projectId, selection]
  );
  return (
    <Form
      layout="vertical"
      ref={formRef}
      initialValues={selection.data}
      onValuesChange={onValuesChange}
    >
      <Item label={t['breack.name']} field="name">
        <EnterBlurInput autoFocus placeholder={t['breack.name.placeholder']} />
      </Item>
      <Item label={t['breack.color']} field="color">
        <ColorInput />
      </Item>
    </Form>
  );
};
