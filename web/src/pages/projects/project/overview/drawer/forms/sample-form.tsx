import { updateFaq } from '@/api/components';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphFaq } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import useLocale from '@/utils/useLocale';
import { Form, Input } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle } from 'react';
import { SelectionProps } from '../../types';
import i18n from '../locale';
import { ComponentHandle } from '../type';

const SampleForm = (
  {
    projectId,
    selection,
    onChange,
    onChangeEditSelection,
  }: SelectionProps<GraphFaq>,
  ref: Ref<ComponentHandle>
) => {
  const formRef = useFormRef(selection.data);
  const t = useLocale(i18n);
  useImperativeHandle(ref, () => ({
    handle: async () => {
      const { initMessage, endMessage, ...other } =
        await formRef.current.validate();
      const data = {
        ...selection.data,
        initMessage: initMessage || t['sample.form.start.placeholder'],
        endMessage: endMessage || t['sample.form.end.placeholder'],
        ...other,
      };
      await updateFaq(projectId, selection.id, data, [projectId]);
      onChange((vals) =>
        ObjectArrayHelper.update(vals, { data }, (f) => f.id === selection.id)
      );
      onChangeEditSelection({
        ...selection,
        data,
      });
    },
  }));
  return (
    <Form layout="vertical" ref={formRef} initialValues={selection.data}>
      <Form.Item label={t['sample.form.start']} field="initMessage">
        <Input.TextArea
          autoSize
          placeholder={t['sample.form.start.placeholder']}
        />
      </Form.Item>
      <Form.Item label={t['sample.form.end']} field="endMessage">
        <Input.TextArea
          autoSize
          placeholder={t['sample.form.end.placeholder']}
        />
      </Form.Item>
    </Form>
  );
};

export default SampleForm;
