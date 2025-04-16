import i18n from '@/pages/projects/locale';
import { isBlank } from '@/utils/is';
import useLocale from '@/utils/useLocale';
import {
  AutoComplete,
  Button,
  Checkbox,
  Divider,
  Empty,
  Form,
  FormInstance,
  Input,
  Space,
  Typography,
} from '@arco-design/web-react';
import { IconClose, IconDelete, IconPlus } from '@arco-design/web-react/icon';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import TextMark from '@/pages/projects/project/conversations/conversation/drawer/components/TextMark';
import { useRequest } from 'ahooks';
import { listFaqLables } from '@/api/faq';
import { BotResponseFormItem } from '../../components/BotResponseFormItem';
import UserExt from '../../components/UserExt';
import GenerateExample from '../../components/GenerateExample';
import { isEmpty, uniqBy } from 'lodash';
import useUrlParams from '../../hooks/useUrlParams';
import TextArea from '../../components/TextArea';

const { Item } = Form;
const config = {
  webhook: false,
};
const getVisibleResponseAnswer = (responses) => {
  if (isEmpty(responses)) return undefined;
  const answer = responses.find(
    (r) => ['text', 'image'].includes(r.type) && !isBlank(r.content.text)
  );
  if (!answer) return undefined;
  return answer.content.text;
};
interface FaqFormProps {
  disabled?: boolean;
  initialValues?: any;
  form?: FormInstance;
}
const FaqForm = (props: FaqFormProps) => {
  const { disabled, initialValues, form } = props;

  const t = useLocale(i18n);
  const { projectId } = useUrlParams();

  const baseResponseRules = useMemo(
    () => [
      {
        required: true,
        minLength: 1,
        message: t['baseResponseRulesMessage'],
      },
    ],
    [t]
  );

  const mainExampleRules = useMemo(
    () => [
      {
        validator(value, callback) {
          if (!value || isBlank(value.text)) {
            callback(t['sample.textRule']);
          }
        },
      },
    ],
    [t]
  );

  const labelsRules = useMemo(
    () => [
      {
        validator(value, callback) {
          if (!value || isBlank(value.text)) {
            callback(t['sample.textRule']);
          }
        },
      },
    ],
    [t]
  );

  const [baseExample, setBaseExample] = useState(
    initialValues?.examples[0]?.text ?? ''
  );

  const { loading, data: labelRes } = useRequest(
    () => listFaqLables(projectId),
    { refreshDeps: [projectId] }
  );
  const originLables = useMemo(
    () => (labelRes ? labelRes.map((l) => l.text) : []),
    [labelRes]
  );

  const onUploadExt = useCallback(
    (val: string[]) => {
      const examples = [
        ...(form?.getFieldValue('examples') || []),
        ...val.map((text) => ({ text, marks: [] })),
      ];
      form?.setFieldValue('examples', examples);
    },
    [form]
  );
  const onGenerated = (intents: string[]) => {
    const origin = form?.getFieldValue('examples') || [];
    const main = form?.getFieldValue('mainExample');
    form?.setFieldValue(
      'examples',
      uniqBy(
        [
          ...intents
            .filter((i) => i !== main)
            .map((text) => ({ text, marks: [] })),
          ...origin,
        ],
        'text'
      )
    );
  };

  const [needAnswer, setNeedAnswer] = useState(() =>
    localStorage.getItem('faq_need_answer') ? true : false
  );
  useEffect(() => {
    setNeedAnswer(
      localStorage.getItem('faq_need_answer') === 'true' ? true : false
    );
  }, []);

  const onGetParams = () => {
    const { mainExample, examples, responses } = form?.getFieldsValue([
      'mainExample',
      'examples',
      'responses',
    ]);
    return {
      intent: mainExample,
      count: 5,
      exts: uniqBy([{ text: mainExample }, ...examples], 'text').map(
        ({ text }) => text
      ),
      answer: needAnswer ? getVisibleResponseAnswer(responses) : undefined,
    };
  };

  const onNeedAnswerChange = (checked) => {
    localStorage.setItem('faq_need_answer', `${checked}`);
    setNeedAnswer(checked);
  };

  const onValuesChange = (field) => {
    if (field.mainExample !== undefined) {
      setBaseExample(field.mainExample);
    }
  };

  const formValues = useMemo(
    () => ({
      ...initialValues,
      mainExample: initialValues?.examples[0]?.text || '',
      examples: initialValues?.examples?.slice(1) || [],
      name: initialValues?.name || initialValues?.examples[0]?.text || '',
    }),
    [initialValues]
  );
  return (
    <Form
      layout="vertical"
      form={form}
      initialValues={formValues}
      onValuesChange={onValuesChange}
    >
      <Item label={t['sample.name']} field="name">
        <Input />
      </Item>
      <Item
        label={`${t['sample.examples']}:`}
        field="mainExample"
        rules={mainExampleRules}
        required
      >
        <TextArea
          autoFocus
          placeholder={t['sample.examples.placeholder']}
          disabled={disabled}
        />
      </Item>
      <Item
        label={
          <div className="flex justify-between items-center">
            <Space>
              <Typography.Text>{t['examples']}</Typography.Text>
              {!disabled && <UserExt onFinish={onUploadExt} />}
            </Space>
            {!disabled && (
              <Space>
                <GenerateExample
                  disabled={!baseExample}
                  onGenerated={onGenerated}
                  onGetParams={onGetParams}
                />
                {baseExample && (
                  <Checkbox
                    checked={needAnswer}
                    onChange={onNeedAnswerChange}
                  />
                )}
                {baseExample && <span>{t['needAnswer']}</span>}
              </Space>
            )}
          </div>
        }
        field="examples"
      >
        <Form.List field="examples">
          {(fields, { add, remove }) => (
            <Space direction="vertical" className="w-full">
              {fields.length === 0 && <Empty description={t['notExamples']} />}
              {fields.map((field, index) => (
                <div key={field.key} className="flex gap-2">
                  <Item
                    key={field.key}
                    field={field.field}
                    rules={mainExampleRules}
                    className="!m-0 w-full"
                  >
                    <TextMark
                      disabled={disabled}
                      canMark={false}
                      placeholder={t['examples.placeholder']}
                    />
                  </Item>
                  {!disabled && (
                    <Button
                      type="outline"
                      status="danger"
                      onClick={() => remove(index)}
                    >
                      <IconDelete />
                    </Button>
                  )}
                </div>
              ))}
              {!disabled && (
                <Button
                  long
                  type="outline"
                  status="success"
                  onClick={() => add({ text: '', marks: [] })}
                >
                  <IconPlus />
                  {t['examples.add']}
                </Button>
              )}
            </Space>
          )}
        </Form.List>
      </Item>
      <Divider />
      <Form.Item shouldUpdate noStyle>
        {({ responses }) => (
          <Item
            label={t['Smart.reply']}
            required
            field="responses"
            rules={baseResponseRules}
          >
            <Form.List field="responses">
              {(fields, operation) => (
                <BotResponseFormItem
                  fields={fields}
                  operation={operation}
                  responses={responses}
                  config={config}
                  disabled={disabled}
                />
              )}
            </Form.List>
          </Item>
        )}
      </Form.Item>
      <Divider />
      <Item label={t['sample.labels']} field={'labels'}>
        <Form.List field="labels">
          {(fields, { add, remove }) => (
            <Space size="small" className="w-full" direction="vertical">
              {fields.map((field, index) => {
                return (
                  <div key={field.key} className="flex items-start gap-2">
                    <Item
                      key={field.key}
                      field={field.field + 'text'}
                      className="!m-0 w-full"
                      rules={labelsRules}
                    >
                      <AutoComplete
                        placeholder={t['sample.labels.placeholder']}
                        strict
                        disabled={disabled}
                        loading={loading}
                        data={originLables}
                      />
                    </Item>
                    {!disabled && (
                      <Button
                        size="mini"
                        shape="circle"
                        status="danger"
                        type="text"
                        onClick={() => remove(index)}
                      >
                        <IconClose />
                      </Button>
                    )}
                  </div>
                );
              })}
              {!disabled && (
                <Space size="mini">
                  <Button
                    type="secondary"
                    size="small"
                    icon={<IconPlus />}
                    onClick={() =>
                      add({
                        text: '',
                      })
                    }
                  >
                    {t['sample.labels.add']}
                  </Button>
                </Space>
              )}
            </Space>
          )}
        </Form.List>
      </Item>
      <Item label={t['description']} field="description">
        <Input.TextArea
          autoSize
          placeholder={t['description.placeholder']}
          disabled={disabled}
        />
      </Item>
    </Form>
  );
};

export default FaqForm;
