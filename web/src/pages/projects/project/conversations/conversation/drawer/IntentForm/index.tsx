import MarkAnnotation from '@/components/MarkAnnotation';
import {
  Example,
  GraphIntentNext,
  IDName,
  IntentMapping,
  IntentNextData,
} from '@/graph-next/type';
import { isBlank } from '@/utils/is';
import useLocale from '@/utils/useLocale';
import {
  Alert,
  Button,
  Form,
  Input,
  Radio,
  Space,
  Switch,
  Tag,
  Typography,
} from '@arco-design/web-react';
import { IconDelete, IconPlus, IconStar } from '@arco-design/web-react/icon';
import { isEmpty, uniqBy } from 'lodash';
import { nanoid } from 'nanoid';
import React, {
  Ref,
  forwardRef,
  useCallback,
  useImperativeHandle,
  useMemo,
  useState,
} from 'react';
import { SelectionProps } from '../../types';
import i18n from './locale';
import { updateIntent } from '../operator';
import { ComponentHandle } from '../type';
import IntentLinkShare from './IntentLinkShare';
import NormalExample from './NormalExample';
import ShareIntentModal from './ShareIntentModal';
import UserExt from '@/pages/projects/project/components/UserExt';
import {
  MultiMappingFormItem,
  SlotsProvider,
  useSlotsContext,
} from '@/pages/projects/project/components/multivariable';
import {
  useInitialEntities,
  useInitialIntentForm,
  useIsGlobalIntent,
  useMappingsEnable,
  useMarkEnable,
} from './hooks';
import { ObjectArrayHelper } from '@/graph-next/helper';
import SetSlotsFormItem from '@/pages/projects/project/components/SetSlotsFormItem';
import overviewMerge from '@/utils/overviewMerge';
import GenerateExample from '@/pages/projects/project/components/GenerateExample';

const { Item } = Form;
const makeExample = (examples: Example[]) =>
  examples
    .filter(Boolean)
    .filter((ex) => !!ex && ex.text && ex.text.trim() !== '');

const createMapping = (): IntentMapping => ({
  id: nanoid(),
  slotId: null,
  slotName: null,
  slotDisplay: null,
  type: null,
  enable: false,
  multiInput: false,
});
const IntentForm = forwardRef(
  (
    {
      projectId,
      selection,
      onChange,
      onChangeEditSelection,
    }: SelectionProps<GraphIntentNext>,
    ref: Ref<ComponentHandle>
  ) => {
    const t = useLocale(i18n);

    const { slots } = useSlotsContext();
    const [initialValues, formRef] = useInitialIntentForm(selection.data);
    const [entities, setEntities] = useInitialEntities(selection.data);

    const isGlobal = useIsGlobalIntent(selection.linkedFrom);

    const [mappingsEnable, onMappingsEnableChange] = useMappingsEnable(
      selection.data,
      () =>
        isEmpty(formRef.current.getFieldValue('mappings')) &&
        formRef.current.setFieldValue('mappings', [createMapping()])
    );
    const [markEnable, onMappingsChange] = useMarkEnable(selection.data);

    const [baseExample, setBaseExample] = useState(
      initialValues.mainExample?.text
    );
    const handleValuesChange = (field, values) => {
      if (field.mappingsEnable !== undefined) {
        onMappingsEnableChange(!!field.mappingsEnable);
      }
      if (
        field.mappings !== undefined ||
        Object.keys(field).some((k) => k.startsWith('mappings['))
      ) {
        onMappingsChange(values.mappings);
      }
      if (field.mainExample !== undefined) {
        setBaseExample(field.mainExample.text);
      }
    };
    const handleShare = (data: IntentNextData, linkedFrom: IDName) => {
      onChangeEditSelection({ ...selection, data, linkedFrom });
    };
    useImperativeHandle(
      ref,
      () => ({
        handle: async () => {
          const {
            mappings,
            mainExample,
            examples = [],
            ...rest
          } = await formRef.current.validate();
          const { id, parentId, relations, linkedFrom, afterRhetorical, data } =
            selection;
          const mps =
            mappings?.map((m) => ({
              ...m,
              enable: rest.mappingsEnable,
            })) || [];
          return updateIntent({
            projectId,
            parentId,
            relations,
            data: {
              ...overviewMerge(rest, data, ['setSlots']),
              mappings: mps,
              examples: makeExample([mainExample, ...examples]),
            },
            id,
            linkedFrom,
            afterRhetorical,
            callback: (node) => {
              onChange((vals) =>
                ObjectArrayHelper.update(vals, node, (f) => f.id === id)
              );
            },
          });
        },
      }),
      [formRef, onChange, projectId, selection]
    );

    const displayOptions = useMemo(
      () => [
        {
          label: t['conversation.intentForm.display.input'],
          value: 'user_input',
        },
        {
          label: t['conversation.intentForm.display.click'],
          value: 'user_click',
        },
      ],
      [t]
    );

    const onUserExtChange = useCallback(
      (exts) => {
        const examples = [
          ...(formRef.current.getFieldValue('examples') || []),
          ...exts.map((text) => ({ text, marks: [] })),
        ];
        formRef.current.setFieldValue('examples', examples);
      },
      [formRef]
    );
    const isFormNodeChild = useMemo(
      () => !!selection.afterRhetorical,
      [selection]
    );
    const onGenerated = (intents: string[]) => {
      const origin = formRef.current.getFieldValue('examples') || [];
      const main = formRef.current.getFieldValue('mainExample');
      formRef.current.setFieldValue(
        'examples',
        uniqBy(
          [
            ...intents
              .filter((i) => i !== main?.text)
              .map((text) => ({ text, marks: [] })),
            ...origin,
          ],
          'text'
        )
      );
    };
    const onGetParams = () => {
      const { mainExample, examples } = formRef.current.getFieldsValue([
        'mainExample',
        'examples',
      ]);
      return {
        intent: mainExample.text,
        count: 5,
        exts: uniqBy([mainExample, ...(examples || [])], 'text').map(
          ({ text }) => text
        ),
      };
    };
    return (
      <Form
        layout="vertical"
        ref={formRef}
        initialValues={initialValues}
        onValuesChange={handleValuesChange}
      >
        <Item
          label={
            <div className="inline-flex justify-center items-center w-[90%]">
              <span>{t['conversation.intentForm.mainExample']}:</span>
              <Space>
                <IntentLinkShare
                  nodeId={selection.id}
                  share={selection.linkedFrom}
                  onChange={handleShare}
                />
                {!isGlobal && (
                  <ShareIntentModal
                    nodeId={selection.id}
                    share={selection.linkedFrom}
                    onChange={handleShare}
                    valueFormRef={formRef}
                  />
                )}
              </Space>
            </div>
          }
          field="mainExample"
          rules={[
            {
              validator(value, callback) {
                if (!value || isBlank(value.text)) {
                  callback(t['conversation.intentForm.mainExample.required']);
                }
              },
            },
          ]}
          required
          extra={
            !!selection.data.name && (
              <span>
                {t['conversation.intentForm.mainExample.label']}:{' '}
                <Tag size="small" color="orange" icon={<IconStar />}>
                  {selection.data.name}
                </Tag>
              </span>
            )
          }
        >
          {markEnable ? (
            <MarkAnnotation
              entities={entities}
              slots={[]}
              placeholder={
                t['conversation.intentForm.mainExample.mark.placeholder']
              }
            />
          ) : (
            <NormalExample
              placeholder={
                t['conversation.intentForm.mainExample.mark.placeholder']
              }
            />
          )}
        </Item>
        <Item
          label={t['conversation.intentForm.display']}
          field="display"
          rules={[{ required: true }]}
        >
          <Radio.Group options={displayOptions} />
        </Item>
        <Item>
          <Space direction="vertical" className="w-full">
            <Alert
              content={t['conversation.intentForm.slotDisabled']}
              action={
                <Item field="mappingsEnable" noStyle triggerPropName="checked">
                  <Switch size="small" type="round" />
                </Item>
              }
            />
            {mappingsEnable && (
              <MultiMappingFormItem
                formRef={formRef}
                field="mappings"
                multiple={!isFormNodeChild}
                partType={!isFormNodeChild}
                rules={[
                  {
                    minLength: 1,
                    message: t['conversation.intentForm.mappings.rule'],
                  },
                ]}
                initialMappings={selection.data.mappings}
                onFromEntityMappingsChange={setEntities}
              />
            )}
          </Space>
        </Item>
        <Item
          label={
            <div className="inline-flex justify-center items-center w-[90%]">
              <Space>
                <Typography.Text>
                  {t['conversation.intentForm.examples.label']}
                </Typography.Text>
                <UserExt onFinish={onUserExtChange} />
                <GenerateExample
                  disabled={!baseExample}
                  onGenerated={onGenerated}
                  onGetParams={onGetParams}
                />
              </Space>
            </div>
          }
          field="examples"
        >
          <Form.List field="examples">
            {(fields, { add, remove }) => (
              <Space direction="vertical" className="w-full">
                {fields.map((field, index) => (
                  <div key={field.key} className="flex gap-2">
                    <Item
                      key={field.key}
                      field={field.field}
                      // rules={textMarkRules}
                      className="!m-0 w-full"
                    >
                      {markEnable ? (
                        <MarkAnnotation
                          entities={entities}
                          slots={[]}
                          placeholder={t['conversation.intentForm.example']}
                        />
                      ) : (
                        <NormalExample
                          placeholder={t['conversation.intentForm.example']}
                        />
                      )}
                    </Item>
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
                  onClick={() => add({ text: '', marks: [], autoFocus: true })}
                >
                  <IconPlus />
                  {t['conversation.intentForm.examples.add']}
                </Button>
              </Space>
            )}
          </Form.List>
        </Item>
        <Item
          label={t['conversation.intentForm.description']}
          field="description"
        >
          <Input.TextArea
            placeholder={t['conversation.intentForm.description.placeholder']}
          />
        </Item>
        <Item label={t['conversation.intentForm.more.actions.reset']}>
          <Form.List field="setSlots">
            {(fields, operation) => (
              <SetSlotsFormItem
                fields={fields}
                operation={operation}
                slots={slots}
              />
            )}
          </Form.List>
        </Item>
      </Form>
    );
  }
);

const IntentFormWithSlots = (
  props: SelectionProps<GraphIntentNext>,
  ref: Ref<ComponentHandle>
) => (
  <SlotsProvider needMap>
    <IntentForm {...props} ref={ref} />
  </SlotsProvider>
);
export default IntentFormWithSlots;
