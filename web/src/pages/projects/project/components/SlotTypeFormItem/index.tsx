import {
  Button,
  Form,
  Input,
  Link,
  Radio,
  Select,
  Tabs,
  Typography,
} from '@arco-design/web-react';
import { IconDelete, IconPlus } from '@arco-design/web-react/icon';
import * as React from 'react';
import TextArea from '../TextArea';
import styled from 'styled-components';
import useRules from '@/hooks/useRules';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

enum VariableInjectType {
  SET = 'set',
  LOCAL_STORE = 'localStore',
  SESSION_STORE = 'sessionStore',
  CUSTOM = 'custom',
}
const LinkWrap = styled(Link)`
  margin-right: 4px;
  &:hover {
    text-decoration: underline;
  }
`;
const exampleFunctionBody = `// example\nconst value = localStorage.getItem('value-key');\nreturn 'example' + value;`;

interface SlotTypeFormItemProps {
  fieldType?: string;
  fieldEnableEnum?: string;
  fieldEnum?: string;
  fieldEnableDefaultValue?: string;
  fieldDefaultValue?: string;
  fieldDefaultValueType?: string;
}
const SlotTypeFormItem: React.FC<SlotTypeFormItemProps> = ({
  fieldDefaultValue = 'defaultValue',
  fieldDefaultValueType = 'defaultValueType',
  fieldEnableDefaultValue = 'defaultValueEnable',
  fieldEnableEnum = 'enumEnable',
  fieldEnum = 'enum',
  fieldType = 'type',
}) => {
  const t = useLocale(i18n);
  const rules = useRules();

  const enablePresetOptions = React.useMemo(
    () => [
      { label: t['slot.form.defaultValue.true'], value: true },
      { label: t['slot.form.defaultValue.false'], value: false },
    ],
    [t]
  );
  const enableSlotTypeOptions = React.useMemo(
    () => [
      { label: t['slot.form.type.enable.true'], value: true },
      { label: t['slot.form.type.enable.false'], value: false },
    ],
    [t]
  );
  return (
    <React.Fragment>
      <Form.Item
        label={t['slot.form.type']}
        field={fieldType}
        rules={rules}
        initialValue="string"
      >
        <Select
          placeholder={t['slot.form.type.placeholder']}
          defaultValue="string"
        >
          <Select.Option value="string">
            {t['slot.form.type.string']}
          </Select.Option>
          <Select.Option value="integer">
            {t['slot.form.type.integer']}
          </Select.Option>
          <Select.Option value="boolean">
            {t['slot.form.type.boolean']}
          </Select.Option>
          <Select.Option value="array">
            {t['slot.form.type.array']}
          </Select.Option>
        </Select>
      </Form.Item>
      <Form.Item
        label={t['slot.form.type.enable']}
        field={fieldEnableEnum}
        initialValue={false}
        rules={rules}
      >
        <Radio.Group options={enableSlotTypeOptions} defaultValue={false} />
      </Form.Item>
      <Form.Item shouldUpdate noStyle>
        {({ enumEnable }) =>
          enumEnable && (
            <Form.Item
              label={t['slot.form.enum']}
              field={fieldEnum}
              rules={rules}
            >
              <Form.List field={fieldEnum}>
                {(fields, { add, remove }) => (
                  <div className="space-y-2">
                    {fields.map((field, index) => (
                      <div key={field.key} className="flex gap-2">
                        <Form.Item field={field.field}>
                          <Input className="flex-1" />
                        </Form.Item>
                        <Button
                          type="outline"
                          status="danger"
                          icon={<IconDelete />}
                          onClick={() => remove(index)}
                        />
                      </div>
                    ))}
                    <Button
                      type="outline"
                      status="success"
                      long
                      onClick={() => add()}
                    >
                      <IconPlus />
                    </Button>
                  </div>
                )}
              </Form.List>
            </Form.Item>
          )
        }
      </Form.Item>
      <Form.Item
        label={t['slot.form.defaultValue.enable']}
        field={fieldEnableDefaultValue}
        initialValue={false}
        rules={rules}
      >
        <Radio.Group options={enablePresetOptions} defaultValue={false} />
      </Form.Item>
      <Form.Item shouldUpdate noStyle>
        {({ defaultValueEnable }) =>
          defaultValueEnable && (
            <Form.Item
              label={t['slot.form.defaultValue']}
              field={fieldDefaultValueType}
              triggerPropName="activeTab"
              initialValue={VariableInjectType.SET}
            >
              <Tabs size="small" defaultActiveTab={VariableInjectType.SET}>
                <Tabs.TabPane
                  key={VariableInjectType.SET}
                  title={t['config.inject.set']}
                >
                  <Form.Item
                    label={t['config.inject.set.body']}
                    field={fieldDefaultValue}
                    rules={rules}
                  >
                    <TextArea placeholder="value" />
                  </Form.Item>
                </Tabs.TabPane>
                <Tabs.TabPane
                  key={VariableInjectType.LOCAL_STORE}
                  title={t['config.inject.localStore']}
                >
                  <Form.Item
                    label={
                      <Typography.Text>
                        <LinkWrap
                          hoverable={false}
                          target="_blank"
                          href="https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage"
                        >
                          {t['config.inject.localStore']}
                        </LinkWrap>
                        {t['config.inject.key']}
                      </Typography.Text>
                    }
                    field={fieldDefaultValue}
                    rules={rules}
                  >
                    <Input placeholder="key" />
                  </Form.Item>
                </Tabs.TabPane>
                <Tabs.TabPane
                  key={VariableInjectType.SESSION_STORE}
                  title={t['config.inject.sessionStore']}
                >
                  <Form.Item
                    label={
                      <Typography.Text>
                        <LinkWrap
                          hoverable={false}
                          target="_blank"
                          href="https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage"
                        >
                          {t['config.inject.sessionStore']}
                        </LinkWrap>
                        {t['config.inject.key']}
                      </Typography.Text>
                    }
                    field={fieldDefaultValue}
                    rules={rules}
                  >
                    <Input placeholder="key" />
                  </Form.Item>
                </Tabs.TabPane>
                <Tabs.TabPane
                  key={VariableInjectType.CUSTOM}
                  title={t['config.custom']}
                >
                  <Form.Item
                    label={t['config.reset.slot.function']}
                    field={fieldDefaultValue}
                    rules={rules}
                  >
                    <Input.TextArea
                      style={{ minHeight: 96 }}
                      placeholder={exampleFunctionBody}
                    />
                  </Form.Item>
                </Tabs.TabPane>
              </Tabs>
            </Form.Item>
          )
        }
      </Form.Item>
    </React.Fragment>
  );
};

export default SlotTypeFormItem;
