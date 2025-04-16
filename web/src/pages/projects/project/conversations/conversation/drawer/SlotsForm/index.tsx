import { updateComponent } from '@/api/components';
import MarkAnnotation from '@/components/MarkAnnotation';
import { GraphSlots } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import { isBlank } from '@/utils/is';
import useLocale from '@/utils/useLocale';
import { Button, Form, RulesProps, Space, Spin } from '@arco-design/web-react';
import { IconDelete, IconPlus } from '@arco-design/web-react/icon';
import { cloneDeep, isEmpty } from 'lodash';
import { nanoid } from 'nanoid';
import React, { Ref, useImperativeHandle, useMemo } from 'react';
import { normalGraphNode } from '../../nodes/util';
import { SelectionProps } from '../../types';
import { useSlots } from '../BotForm/hook';
import CodeField from '../FieldForm/components/CodeField';
import i18n from '../IntentForm/locale';
import { ComponentHandle } from '../type';
import useDocumentLinks from '@/hooks/useDocumentLinks';

const defaultValue = `# don't edit this function\`s name
async def required_slots(
    self,
    domain_slots: List[Text],
    dispatcher: CollectingDispatcher,
    tracker: Tracker,
    domain: DomainDict,) -> List[Text]:

    # additional_slots = ["append_slot_name"]
    # if tracker.slots.get("append_slot_name") is True:
      # If the user wants to sit outside, ask
      # if they want to sit in the shade or in the sun.
      # additional_slots.append("append_other_slot_name")
      # return required_slots
    
    return domain_slots`;
const SlotsForm = (
  { projectId, selection, onChangeEditSelection }: SelectionProps<GraphSlots>,
  ref: Ref<ComponentHandle>
) => {
  const t = useLocale(i18n);

  const mainExampleRules: RulesProps[] = useMemo(
    () => [
      {
        validator(value, callback) {
          if (!value || isBlank(value.text)) {
            callback(t['conversation.intentForm.mainExample.required']);
          }
        },
      },
    ],
    [t]
  );

  const initialValues = useMemo(() => {
    const clone = cloneDeep([...(selection.data?.examples || [])]);
    const mainExample = clone.shift();
    const value = {
      examples: clone,
      mainExample,
    };
    return value;
  }, [selection.data]);

  const formRef = useFormRef(initialValues);

  useImperativeHandle(
    ref,
    () => ({
      handle: async () => {
        return updateComponent(
          projectId,
          'slots',
          selection.id,
          normalGraphNode(selection)
        );
      },
    }),
    [projectId, selection]
  );

  const handleValuesChange = (_, values) => {
    const { mainExample, examples, ...rest } = values;
    const exs = [mainExample, ...examples].filter(Boolean);
    onChangeEditSelection({
      ...selection,
      data: { ...selection.data, examples: exs, ...rest },
    });
  };

  const { loading, slots: originalEntities } = useSlots(projectId);
  const slots = useMemo(() => {
    return (
      selection.children
        .filter((o) => o.type === 'field')
        // create a fake slot node but id is the field node's id
        .map((o) => ({ slotId: o.id, slotDisplay: o.data.slotDisplay }))
    );
  }, [selection]);
  const entities = useMemo(
    () =>
      originalEntities?.map(({ id, display, name }) => ({
        id: nanoid(),
        slotId: id,
        slotDisplay: display,
        slotName: name,
      })),
    [originalEntities]
  );
  const mutilSlotVariable = useMemo(
    () =>
      !isEmpty(selection.children) &&
      selection.children.some((d) => !isEmpty(d.data.slotId)),
    [selection]
  );
  const docs = useDocumentLinks();
  return (
    <Spin loading={loading} className="w-full">
      <Form
        layout="vertical"
        ref={formRef}
        initialValues={initialValues}
        onValuesChange={handleValuesChange}
      >
        {mutilSlotVariable && (
          <>
            <Form.Item
              label={t['conversation.intentForm.mainExample']}
              field="mainExample"
              rules={mainExampleRules}
            >
              <MarkAnnotation
                entities={entities}
                slots={slots}
                placeholder={
                  t['conversation.intentForm.mainExample.mark.placeholder']
                }
              />
            </Form.Item>
            <Form.Item
              label={`${t['conversation.intentForm.examples.label']}:`}
              field="examples"
            >
              <Form.List field="examples">
                {(fields, { add, remove }) => (
                  <Space direction="vertical" className="w-full">
                    {fields.map((field, index) => (
                      <div key={field.key} className="flex gap-2">
                        <Form.Item
                          key={field.key}
                          field={field.field}
                          className="!m-0 w-full"
                        >
                          <MarkAnnotation
                            entities={entities}
                            slots={slots}
                            placeholder={t['conversation.intentForm.example']}
                          />
                        </Form.Item>
                        <Button
                          type="text"
                          status="danger"
                          onClick={() => remove(index)}
                        >
                          <IconDelete />
                        </Button>
                      </div>
                    ))}
                    <Button
                      long
                      type="outline"
                      status="success"
                      onClick={() =>
                        add({ text: '', marks: [], autoFocus: true })
                      }
                    >
                      <IconPlus />
                      {t['conversation.intentForm.examples.add']}
                    </Button>
                  </Space>
                )}
              </Form.List>
            </Form.Item>
          </>
        )}
        <Form.Item
          label={t['conversation.intentForm.required.slots.code']}
          field="requiredSlotsCode"
        >
          <CodeField
            defaultValue={defaultValue}
            title={t['conversation.intentForm.required.slots.code']}
            titleLink={docs.dynamicSlot}
          />
        </Form.Item>
      </Form>
    </Spin>
  );
};

export default SlotsForm;
