import { createGlobalIntent, updateGlobalIntent } from '@/api/global-component';
import { Example, GlobalIntent, IntentMapping } from '@/graph-next/type';
import { isBlank } from '@/utils/is';
import useLocale from '@/utils/useLocale';
import {
  Alert,
  Button,
  Form,
  Input,
  Message,
  Modal,
  RulesProps,
  Space,
  Switch,
  Tooltip,
  Typography,
} from '@arco-design/web-react';
import { IconDelete, IconEdit, IconPlus } from '@arco-design/web-react/icon';
import { isEmpty, uniqBy } from 'lodash';
import { nanoid } from 'nanoid';
import React, { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router';
import i18n from '../../locale';
import MarkAnnotation from '@/components/MarkAnnotation';
import NormalExample from '../../../conversations/conversation/drawer/IntentForm/NormalExample';
import useRules from '@/hooks/useRules';
import UserExt from '../../../components/UserExt';
import {
  MultiMappingFormItem,
  SlotsProvider,
} from '@/pages/projects/project/components/multivariable';
import {
  useInitialIntentForm,
  useInitialEntities,
  useMappingsEnable,
  useMarkEnable,
} from '../../../conversations/conversation/drawer/IntentForm/hooks';
import GenerateExample from '../../../components/GenerateExample';

interface IntentModalProps {
  value?: GlobalIntent;
  callback: () => void;
}

const createMapping = (): IntentMapping => ({
  id: nanoid(),
  slotId: null,
  slotName: null,
  slotDisplay: null,
  type: null,
  enable: false,
  multiInput: false,
});
const makeExample = (examples: Example[]) =>
  examples.filter((ex) => !!ex && ex.text && ex.text.trim() !== '');
const { Item } = Form;
const IntentModal = ({ value, callback }: IntentModalProps) => {
  const { id: projectId } = useParams<{ id: string }>();
  const t = useLocale(i18n);
  const rules = useRules();

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
  const [initialValues, formRef] = useInitialIntentForm(value?.data);
  const [entities, setEntities] = useInitialEntities(value?.data);
  const [mappingsEnable, onMappingsEnableChange] = useMappingsEnable(
    value?.data,
    () =>
      isEmpty(formRef.current.getFieldValue('mappings')) &&
      formRef.current.setFieldValue('mappings', [createMapping()])
  );
  const [markEnable, onMappingsChange] = useMarkEnable(value?.data);

  const [visible, setVisible] = useState(false);

  const [loading, setLoading] = useState(false);
  const handleOk = async () => {
    const {
      mainExample,
      examples = [],
      mappings,
      ...rest
    } = await formRef.current.validate();
    const saveData = {
      ...rest,
      examples: makeExample([mainExample, ...examples]),
      mappings:
        mappings?.map((m) => ({
          ...m,
          enable: true,
        })) || [],
    };
    if (value) {
      setLoading(true);
      await updateGlobalIntent(projectId, {
        ...value,
        data: saveData,
      })
        .then(() => {
          Message.success(t['save.success']);
          setVisible(false);
          callback?.();
        })
        .finally(() => setLoading(false));
    } else {
      // add
      setLoading(true);
      createGlobalIntent(projectId, {
        id: null,
        type: 'user-global',
        data: saveData,
        componentRelation: {
          usedByComponentRoots: [],
        },
      })
        .then(() => {
          Message.success(t['save.success']);
          setVisible(false);
          callback?.();
        })
        .finally(() => setLoading(false));
    }
  };

  const [baseExample, setBaseExample] = useState(
    initialValues.mainExample?.text
  );
  const onValuesChange = (field, values) => {
    if (visible) {
      if (field['mappingsEnable'] !== undefined) {
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
    }
  };
  // const displayOptions = useMemo(
  //   () => [
  //     {
  //       label: t['intents.form.display.input'],
  //       value: 'user_input',
  //     },
  //     {
  //       label: t['intents.form.display.click'],
  //       value: 'user_click',
  //     },
  //   ],
  //   [t]
  // );
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
    <div>
      <Button
        type="text"
        size={value ? 'mini' : 'default'}
        onClick={() => setVisible(true)}
      >
        {value ? (
          <Tooltip content={t['intents.modal.edit']}>
            <IconEdit />
          </Tooltip>
        ) : (
          <div>
            <IconPlus />
            {t['intents.modal.create']}
          </div>
        )}
      </Button>
      <Modal
        title={
          value ? t['intents.modal.editTitle'] : t['intents.modal.createTitle']
        }
        style={{ width: '45%' }}
        visible={visible}
        onCancel={() => setVisible(false)}
        onOk={handleOk}
        unmountOnExit
        confirmLoading={loading}
      >
        <Form
          layout="vertical"
          ref={formRef}
          initialValues={initialValues}
          onValuesChange={onValuesChange}
        >
          <Item label={`${t['intents.form.name']}:`} field="name" rules={rules}>
            <Input autoFocus placeholder={t['intents.form.name.placeholder']} />
          </Item>
          <Item
            label={`${t['intents.form.mainExample']}:`}
            field="mainExample"
            rules={mainExampleRules}
            required
          >
            {markEnable ? (
              <MarkAnnotation
                entities={entities}
                slots={[]}
                placeholder={t['intents.form.mask.placeholder']}
              />
            ) : (
              <NormalExample placeholder={t['intents.form.mask.placeholder']} />
            )}
          </Item>
          {/* <Item
            label={t['conversation.intentForm.display']}
            field="display"
            rules={[{ required: true }]}
          >
            <Radio.Group options={displayOptions} />
          </Item> */}
          <Item>
            <Space direction="vertical" className="w-full">
              <Alert
                content={t['intents.form.slotVisible']}
                action={
                  <Item
                    field="mappingsEnable"
                    noStyle
                    triggerPropName="checked"
                  >
                    <Switch size="small" type="round" />
                  </Item>
                }
              />
              {mappingsEnable && (
                <MultiMappingFormItem
                  formRef={formRef}
                  field="mappings"
                  multiple
                  partType={false}
                  rules={[
                    {
                      minLength: 1,
                      message: t['intents.form.mappings.rule'],
                    },
                  ]}
                  initialMappings={value?.data.mappings}
                  onFromEntityMappingsChange={setEntities}
                />
              )}
            </Space>
          </Item>
          <Item
            label={
              <Space>
                <Typography.Text>{t['intents.form.examples']}</Typography.Text>
                <UserExt onFinish={onUserExtChange} />
                <GenerateExample
                  disabled={!baseExample}
                  onGenerated={onGenerated}
                  onGetParams={onGetParams}
                />
              </Space>
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
                        className="!m-0 w-full"
                      >
                        {markEnable ? (
                          <MarkAnnotation
                            entities={entities}
                            slots={[]}
                            placeholder={t['intents.form.mask.placeholder']}
                          />
                        ) : (
                          <NormalExample
                            placeholder={t['intents.form.mask.placeholder']}
                          />
                        )}
                      </Item>
                      <Button
                        type="outline"
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
                    onClick={() => add({ text: '', marks: [] })}
                  >
                    <IconPlus />
                    {t['intents.form.examples.add']}
                  </Button>
                </Space>
              )}
            </Form.List>
          </Item>
          <Item label={t['intents.form.description']} field="description">
            <Input.TextArea
              placeholder={t['intents.form.description.placeholder']}
            />
          </Item>
        </Form>
      </Modal>
    </div>
  );
};

export default (props) => (
  <SlotsProvider needMap>
    <IntentModal {...props} />
  </SlotsProvider>
);
