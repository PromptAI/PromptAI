import { GraphBreak } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import { Form } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle } from 'react';
import { SelectionProps } from '../../types';
import { updateCondition } from '../operator';
import ColorInput from '../BreakForm/ColorInput';
import produce from 'immer';
import { ComponentHandle } from '../type';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
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
        return updateCondition({
          projectId,
          id,
          parentId,
          relations,
          data,
          callback: (node) => {
            onChange(
              produce((nodes) => {
                const conditionNodeIndex = nodes.findIndex(
                  (o) => o.id === node.id
                );
                if (conditionNodeIndex > -1) {
                  nodes[conditionNodeIndex] = node;
                }

                const breakIndex = nodes.findIndex(
                  (o) => o.type === 'break' && o.data.conditionId === node.id
                );

                if (breakIndex > -1) {
                  nodes[breakIndex].data.name = node.data.name;
                  nodes[breakIndex].data.color = node.data.color;
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
      <Item label={t['condition.name']} field="name">
        <EnterBlurInput
          autoFocus
          placeholder={t['condition.name.placeholder']}
        />
      </Item>
      <Item label={t['condition.color']} field="color">
        <ColorInput />
      </Item>
    </Form>
  );
};
