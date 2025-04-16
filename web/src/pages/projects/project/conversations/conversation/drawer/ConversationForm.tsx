import { useDynamicMenu } from '@/components/Layout/dynamic-menu-context';
import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphConversation } from '@/graph-next/type';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import { Form, FormInstance, Input, Space } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle, useRef } from 'react';
import EnterBlurInput from '../../../components/EnterBlurInput';
import { SelectionProps } from '../types';
import i18n from './locale';
import { updateFlowRoot } from './operator';
import { ComponentHandle } from './type';
import { IconQuestionCircle } from '@arco-design/web-react/icon';
import useDocumentLinks from '@/hooks/useDocumentLinks';

const { Item } = Form;
const ConversationForm = (
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
      <Item label={t['ConversationForm.name']} field="name" rules={rules}>
        <EnterBlurInput
          autoFocus
          placeholder={t['ConversationForm.name.placeholder']}
        />
      </Item>
      <Item label={t['ConversationForm.description']} field="description">
        <Input.TextArea
          placeholder={t['ConversationForm.description.placeholder']}
          autoSize
          style={{ minHeight: 64 }}
        />
      </Item>
      {/* <Item
        label={t['ConversationForm.blnShowOptional']}
        field="hidden"
        disabled={!selection.data?.canEditorHidden}
        triggerPropName="checked"
        layout="inline"
      >
        <Switch
          type="round"
          checkedText={t['ConversationForm.blnShowOptional.true']}
          uncheckedText={t['ConversationForm.blnShowOptional.false']}
        >
          {t['ConversationForm.blnShowOptional.placeholder']}
        </Switch>
      </Item> */}
      <Item
        label={
          <Space>
            <span>{t['ConversationForm.welcome']}</span>
            <a target="_blank" href={docs.projectSettings} rel="noreferrer">
              <IconQuestionCircle />
            </a>
          </Space>
        }
        field="welcome"
      >
        <Input.TextArea
          placeholder={t['ConversationForm.welcome.placeholder']}
          autoSize
          style={{ minHeight: 64 }}
        />
      </Item>
    </Form>
  );
};

export default ConversationForm;
