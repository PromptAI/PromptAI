import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphRhetoricalNext } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import { isBlank } from '@/utils/is';
import { Form } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle } from 'react';
import { SelectionProps } from '../types';
import { Text } from '@/pages/projects/project/components/BotResponseFormItem';
import { updateRhetoricalNode } from './operator';
import { ComponentHandle } from './type';
import useLocale from '@/utils/useLocale';
import i18n from '@/pages/projects/locale';

const { Item } = Form;

const RhetoricalForm = (
  {
    projectId,
    selection,
    onChangeEditSelection,
    onChange,
  }: SelectionProps<GraphRhetoricalNext>,
  ref: Ref<ComponentHandle>
) => {
  const formRef = useFormRef(selection.data);
  const t = useLocale(i18n);
  const rules = [
    {
      validator: (value, callback) => {
        if (isBlank(value?.content?.text || '')) callback(t['text.warning']);
      },
    },
  ];
  useImperativeHandle(ref, () => ({
    handle: async () => {
      await formRef.current.validate();
      const { id, parentId, relations, data } = selection;
      return updateRhetoricalNode({
        projectId,
        id,
        parentId,
        relations,
        data,
        callback: (node) => {
          onChange((vals) =>
            ObjectArrayHelper.update(vals, node, (f) => f.id === id)
          );
        },
      });
    },
  }));
  const onValuesChange = (_, values) => {
    onChangeEditSelection({ ...selection, data: values });
  };
  return (
    <Form
      ref={formRef}
      layout="vertical"
      initialValues={selection.data}
      onValuesChange={onValuesChange}
    >
      <Item label={`${t['doAsk']}:`} field="responses[0]" rules={rules}>
        <Text placeholder={t['doAsk.placeholder']} />
      </Item>
      {/* <Item label={t['doAsk.description']} field="description">
        <Input.TextArea autoSize placeholder={t['doAsk.description']} />
      </Item> */}
    </Form>
  );
};

export default RhetoricalForm;
