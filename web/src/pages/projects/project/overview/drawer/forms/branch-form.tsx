import { useDynamicMenu } from '@/components/Layout/dynamic-menu-context';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphConversation } from '@/graph-next/type';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import { Form, FormInstance, Input, Space } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle, useRef } from 'react';
import EnterBlurInput from '../../../components/EnterBlurInput';
import i18n from '../locale';
import { ComponentHandle } from '../type';
import { updateConversation } from '@/api/components';
import nProgress from 'nprogress';
import { IconQuestionCircle } from '@arco-design/web-react/icon';
import useDocumentLinks from '@/hooks/useDocumentLinks';
import { SelectionProps } from '../../types';

export async function updateFlowRoot({
  projectId,
  id,
  data,
  parentId,
  relations,
  callback,
}) {
  nProgress.start();
  return updateConversation(projectId, id, data, relations, '')
    .then(({ validatorError }) => {
      callback({
        id,
        parentId,
        type: 'conversation',
        data,
        relations,
        validatorError,
      });
    })
    .finally(nProgress.done);
}

const { Item } = Form;
const BranchForm = (
  {
    projectId,
    selection,
    onChange,
    onChangeEditSelection,
  }: SelectionProps<GraphConversation>,
  ref: Ref<ComponentHandle>
) => {
  const t = useLocale(i18n);
  const formRef = useRef<FormInstance>();
  const rules = useRules();
  const { refresh } = useDynamicMenu();
  useImperativeHandle(
    ref,
    () => ({
      handle: async () => {
        await formRef.current.validate();
        const { id, parentId, relations, data } = selection;
        await updateFlowRoot({
          projectId,
          id,
          parentId,
          data: { ...data, hidden: !data.hidden },
          relations,
          callback: (node) => {
            onChange((vals) =>
              ObjectArrayHelper.update(vals, node, (v) => v.id === id)
            );
          },
        });
        refresh();
      },
    }),
    [onChange, projectId, refresh, selection]
  );
  const onValuesChange = (_, values) => {
    onChangeEditSelection({ ...selection, data: values });
  };
  const docs = useDocumentLinks();
  return (
    <Form
      layout="vertical"
      ref={formRef}
      initialValues={{ ...selection.data, hidden: !selection.data.hidden }}
      onValuesChange={onValuesChange}
    >
      <Item label={t['branch.form.name']} field="name" rules={rules}>
        <EnterBlurInput
          autoFocus
          placeholder={t['branch.form.name.placeholder']}
        />
      </Item>
      <Item label={t['branch.form.description']} field="description">
        <Input.TextArea
          placeholder={t['branch.form.description.placeholder']}
          autoSize
          style={{ minHeight: 64 }}
        />
      </Item>
      {/* <Item
        label={t['branch.form.blnShowOptional']}
        field="hidden"
        disabled={!selection.data?.canEditorHidden}
        triggerPropName="checked"
        layout="inline"
      >
        <Switch
          type="round"
          checkedText={t['branch.form.blnShowOptional.true']}
          uncheckedText={t['branch.form.blnShowOptional.false']}
        >
          {t['branch.form.blnShowOptional.placeholder']}
        </Switch>
      </Item> */}
      <Item
        label={
          <Space>
            <span>{t['branch.form.welcome']}</span>
            <a target="_blank" href={docs.projectSettings} rel="noreferrer">
              <IconQuestionCircle />
            </a>
          </Space>
        }
        field="welcome"
      >
        <Input.TextArea
          placeholder={t['branch.form.welcome.placeholder']}
          autoSize
          style={{ minHeight: 64 }}
        />
      </Item>
    </Form>
  );
};

export default BranchForm;
