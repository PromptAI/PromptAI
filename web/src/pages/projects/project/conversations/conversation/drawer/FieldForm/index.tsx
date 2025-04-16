import { ObjectArrayHelper } from '@/graph-next/helper';
import { GraphField } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import useRules from '@/hooks/useRules';
import { Alert, Form, Input } from '@arco-design/web-react';
import React, { Ref, useImperativeHandle, useState } from 'react';
import { SelectionProps } from '../../types';
import { updateFieldNode } from '../operator';
import { ComponentHandle } from '../type';
import useSlots from '../IntentForm/useSlots';
import SlotSelect from '../IntentForm/SlotSelect';
import { isEmpty } from 'lodash';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import CodeField from './components/CodeField';

const { Item } = Form;

const validateDefaultValue = `# \${slot_name} will be automatically replaced by the system
def validate_\${slot_name}(
    self,
    slot_value: Any,
    dispatcher: CollectingDispatcher,
    tracker: Tracker,
    domain: DomainDict,) -> Dict[Text, Any]:

    """Validate slot_name value."""
    
    return {"\${slot_name}": slot_value}`;

const extractDefaultValue = `# don't edit this function\`s name
async def extract_\${slot_name}(
    self,
    dispatcher: CollectingDispatcher,
    tracker: Tracker,
    domain: Dict,) -> Dict[Text, Any]:

    text_of_last_user_message = tracker.latest_message.get("text")
    sit_outside = "outdoor" in text_of_last_user_message
    
    return {"\${slot_name}": sit_outside}`;
const canbeChangeSlot = (selection) => {
  if (
    selection.children &&
    selection.children[0] &&
    selection.children[0].children?.[0]
  ) {
    const definedCollectNode = selection.children[0].children[0];
    if (
      definedCollectNode.data.mappingsEnable &&
      !isEmpty(definedCollectNode.data.mappings)
    ) {
      return false;
    }
  }
  return true;
};
const FieldForm = (
  {
    projectId,
    selection,
    onChange,
    onChangeEditSelection,
  }: SelectionProps<GraphField>,
  ref: Ref<ComponentHandle>
) => {
  const t = useLocale(i18n);
  const formRef = useFormRef(selection.data);
  const rules = useRules();

  const { loading, slots, slotMap, refresh } = useSlots(projectId);

  const [alertMessage, setAlertMessage] = useState<string>(null);

  useImperativeHandle(
    ref,
    () => ({
      handle: async () => {
        await formRef.current.validate();
        if (alertMessage) {
          return Promise.reject(t['drawer.field.form.error.1']);
        }
        const { id, parentId, relations, data } = selection;
        const slotName = slotMap[data.slotId]?.name;
        return updateFieldNode({
          projectId,
          id,
          parentId,
          relations,
          data: { ...data, slotName },
          callback: (node) => {
            onChange((vals) =>
              ObjectArrayHelper.update(vals, node, (v) => v.id === id)
            );
          },
        });
      },
    }),
    [alertMessage, formRef, onChange, projectId, selection, slotMap, t]
  );
  const onValuesChange = (field, values) => {
    if (!loading) {
      if (field.slotId && field.slotId !== selection.data.slotId) {
        if (!canbeChangeSlot(selection)) {
          setAlertMessage(t['drawer.field.form.error.1']);
          formRef.current.setFieldValue('slotId', selection.data.slotId);
          return;
        }
      }
      onChangeEditSelection({ ...selection, data: values });
    }
  };
  const onCreatedSlot = (id) => {
    if (selection.data.slotId) {
      if (!canbeChangeSlot(selection)) {
        setAlertMessage(t['drawer.field.form.error.1']);
        formRef.current.setFieldValue('slotId', selection.data.slotId);
        return;
      }
    }
    onChangeEditSelection({
      ...selection,
      data: { ...selection.data, slotId: id },
    });
  };
  return (
    <Form
      layout="vertical"
      ref={formRef}
      initialValues={selection.data}
      onValuesChange={onValuesChange}
    >
      {alertMessage && <Alert type="error" content={alertMessage} />}
      <Item
        label={t['drawer.field.form.slotName']}
        field="slotId"
        rules={rules}
      >
        <SlotSelect
          projectId={projectId}
          loading={loading}
          refreshOptions={refresh}
          options={slots}
          onCreated={onCreatedSlot}
        />
      </Item>
      <Form.Item label={t['drawer.field.form.code']} field="validatedCode">
        <CodeField
          defaultValue={validateDefaultValue}
          title={t['drawer.field.form.code']}
        />
      </Form.Item>
      <Form.Item label={t['drawer.field.form.extract']} field="extractCode">
        <CodeField
          defaultValue={extractDefaultValue}
          title={t['drawer.field.form.extract']}
        />
      </Form.Item>
      <Item label={t['drawer.field.form.description']} field="description">
        <Input.TextArea
          placeholder={t['drawer.field.form.description']}
          autoSize
        />
      </Item>
    </Form>
  );
};

export default FieldForm;
