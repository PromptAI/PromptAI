import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Button,
  Checkbox,
  Divider,
  Form,
  FormInstance,
  Grid,
  Input,
  Link,
  Modal,
  Radio,
  Select,
  Space,
  Tabs,
  Typography,
} from '@arco-design/web-react';
import { useToggle } from 'ahooks';
import {
  IconDelete,
  IconEyeInvisible,
  IconPlus,
} from '@arco-design/web-react/icon';
import styles from './index.module.less';
import { Settings } from '../../type';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { decodeChatUrl, getConfig } from '@/utils/chatsdk';
import { debounce, isEmpty } from 'lodash';
import UploadSdkAvatar from './UploadSdkAvatar';
import useRules from '@/hooks/useRules';
import styled from 'styled-components';
import ThemeItem, { ThemeItemProps } from './ThemeItem';
import TextArea from '../../../components/TextArea';
import Runtime from '@/utils/runtime';
import Singleton from '@/utils/singleton';

const isPro = process.env.NODE_ENV === 'production';
const DefaultBotAvatar = () => (
  <svg
    viewBox="0 0 1024 1024"
    width="1.25em"
    height="1.25em"
    version="1.1"
    xmlns="http://www.w3.org/2000/svg"
    style={{ color: 'rgb(51, 103, 255)', fill: 'currentcolor' }}
  >
    <path d="M780.1 256l32.2-55.7c10.9-18.8 4.4-42.9-14.4-53.8-18.9-10.9-42.9-4.4-53.8 14.4l-39.4 68.2c-4.3 7.5-5.2 15.7-4.4 23.7H323.7c0.8-8 0-16.2-4.4-23.7L280 160.8c-10.9-18.8-35-25.3-53.8-14.4-18.8 10.9-25.3 35-14.4 53.8l32.2 55.7c-71.8 14.7-125.7 78.2-125.7 154.3v315.1c0 87 70.5 157.5 157.5 157.5h472.6c87 0 157.5-70.5 157.5-157.5v-315c-0.1-76.1-54-139.6-125.8-154.3z m47 469.3c0 43.4-35.3 78.8-78.8 78.8H275.7c-43.4 0-78.8-35.3-78.8-78.8v-315c0-43.4 35.3-78.8 78.8-78.8h472.6c43.4 0 78.8 35.3 78.8 78.8v315zM39.4 449.6C17.6 449.6 0 467.3 0 489v157.5c0 21.8 17.6 39.4 39.4 39.4s39.4-17.6 39.4-39.4V489c0-21.7-17.7-39.4-39.4-39.4zM984.6 449.6c-21.8 0-39.4 17.6-39.4 39.4v157.5c0 21.8 17.6 39.4 39.4 39.4s39.4-17.6 39.4-39.4V489c0-21.7-17.6-39.4-39.4-39.4z"></path>
    <path d="M382.4 552.4m-59.1 0a59.1 59.1 0 1 0 118.2 0 59.1 59.1 0 1 0-118.2 0Z"></path>
    <path d="M641.6 555.9m-59.1 0a59.1 59.1 0 1 0 118.2 0 59.1 59.1 0 1 0-118.2 0Z"></path>
  </svg>
);

const exampleFunctionBody = `// example\nconst value = localStorage.getItem('value-key');\nreturn 'example' + value;`;

interface BotSettingsModalProps {
  type: 'web' | 'mobile';
  initialValues: Settings;
  mobileUrl?: string;
  onSettingsChange?: (settings: Settings) => void;
}

const computeSrc = (config, mobileUrl) => {
  if (isEmpty(mobileUrl)) return '';
  const originSettings = decodeChatUrl(mobileUrl);
  return `${
    isPro ? window.location.origin : 'http://localhost:3003'
  }/ava/?preview=true&${getConfig({
    ...originSettings,
    ...config,
  })}&${new URLSearchParams(config).toString()}`;
};
const validateImageUrl = async (value: string) =>
  new Promise((r, rj) => {
    const image = new Image();
    image.src = value;
    image.onerror = () => rj('loading image is error');
    image.onload = () => {
      image.remove();
      r(undefined);
    };
    document.body.appendChild(image);
  });

const BotSettingsModal: React.FC<BotSettingsModalProps> = ({
  type,
  initialValues,
  mobileUrl,
  onSettingsChange,
}) => {
  const t = useLocale(i18n);
  const formRef = useRef<FormInstance>();
  const [visible, { toggle }] = useToggle(false);

  const onSubmit = async () => {
    const values = await formRef.current.validate();
    if (values.icon?.bot) {
      await validateImageUrl(values.icon.bot);
    }
    onSettingsChange?.(values);
    toggle();
  };

  const [previewSrc, setPreviewSrc] = useState(() =>
    computeSrc(initialValues, mobileUrl)
  );
  useEffect(() => {
    if (visible) {
      setPreviewSrc(computeSrc(initialValues, mobileUrl));
    }
  }, [initialValues, mobileUrl, visible]);
  const debounceCompute = useMemo(
    () =>
      debounce((config) => {
        const errors = formRef.current.getFieldsError();
        if (isEmpty(errors)) {
          setPreviewSrc(computeSrc(config, mobileUrl));
        }
      }, 500),
    [mobileUrl]
  );

  const themes = useMemo<(ThemeItemProps & { key: string })[]>(
    () => [
      {
        key: 'default',
        primary: '#0041ff',
        name: t['config.theme.default.name'],
        borderRadius: 0,
        description: t['config.theme.default.description'],
      },
      {
        key: 'linear-sky',
        primary: 'linear-gradient(90deg, #1b62e8,#08abf7)',
        name: t['config.theme.linear-sky.name'],
        borderRadius: 8,
        description: t['config.theme.linear-sky.description'],
      },
    ],
    [t]
  );

  return (
    <>
      <Button type="primary" onClick={toggle}>
        {t['button.label']}
      </Button>
      <Modal
        style={{ width: '64%' }}
        title={t['modal.title']}
        unmountOnExit
        visible={visible}
        onOk={onSubmit}
        onCancel={toggle}
      >
        <Form
          ref={formRef}
          initialValues={initialValues}
          layout="vertical"
          onValuesChange={(_, values) => debounceCompute(values)}
        >
          <Grid.Row gutter={24}>
            <Grid.Col span={12}>
              <Tabs defaultActiveTab="base">
                <Tabs.TabPane key="base" title={t['config.base']}>
                  <Form.Item
                    label={t['config.name']}
                    field="name"
                    rules={[
                      {
                        type: 'string',
                        maxLength: 40,
                        message: t['config.name.rule'],
                      },
                    ]}
                  >
                    <Input placeholder="Prompt AI" />
                  </Form.Item>
                  <Form.Item label={t['config.locale']} field="locale">
                    <Select
                      options={[
                        { label: '中文', value: 'zh' },
                        { label: 'English', value: 'en' },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item label={t['config.welcome']} field="welcome">
                    <Input.TextArea
                      placeholder={t['config.welcome.placeholder']}
                      style={{ minHeight: 32 }}
                    />
                  </Form.Item>
                  <Form.Item
                    label={t['config.icon.bot']}
                    field="icon.bot"
                    rules={[
                      {
                        validator(value) {
                          if (!value) return;
                          validateImageUrl(value).catch(() =>
                            formRef.current.setFields({
                              'icon.bot': {
                                value,
                                error: {
                                  message: t['config.icon.bot.input.rule'],
                                },
                              },
                            })
                          );
                        },
                      },
                    ]}
                  >
                    <UploadSdkAvatar alt={<DefaultBotAvatar />} />
                  </Form.Item>
                 <div className={'flex'}>
                   <Form.Item
                       label={t['config.survey']}
                       field="survey"
                       triggerPropName="checked"
                   >
                     <Checkbox>{t['boolean.open']}</Checkbox>
                   </Form.Item>
                   <Form.Item
                       label={t['config.upload']}
                       field="upload"
                       triggerPropName="checked"
                       style={{ marginBottom: 0 }}
                   >
                     <Checkbox>{t['boolean.open']}</Checkbox>
                   </Form.Item>
                   <Form.Item
                       label={t['config.minimize']}
                       field="minimize"
                       triggerPropName="checked"
                       style={{ marginBottom: 0 }}
                   >
                     <Checkbox>{t['boolean.open']}</Checkbox>
                   </Form.Item>
                   <Form.Item
                       label={t['config.schedule.option']}
                       field="schedule"
                       rules={[{ required: true, message: t['config.schedule.option.required'] }]}
                   >
                     <Select
                       placeholder={t['config.schedule.option.placeholder']}
                       defaultValue="priority"
                       options={[
                         { label: t['config.schedule.option.dispatcher'], value: 'dispatcher' },
                         { label: t['config.schedule.option.priority'], value: 'priority' },
                       ]}
                     />
                   </Form.Item>
                 </div>
                </Tabs.TabPane>
                <Tabs.TabPane key="theme" title={t['config.theme']}>
                  <Form.Item field="theme">
                    <Radio.Group className="w-full">
                      <Space direction="vertical" className="w-full">
                        {themes.map((props) => (
                          <Radio
                            key={props.key}
                            value={props.key}
                            className="w-full"
                          >
                            {({ checked }) => (
                              <ThemeItem {...props} checked={checked} />
                            )}
                          </Radio>
                        ))}
                      </Space>
                    </Radio.Group>
                  </Form.Item>
                </Tabs.TabPane>
              </Tabs>
            </Grid.Col>
            <Grid.Col span={12}>
              {previewSrc && (
                <iframe
                  src={previewSrc}
                  style={{
                    width: '100%',
                    border: 'none',
                    height: 520,
                    boxShadow: '0px 0px 8px var(--color-fill-2)',
                  }}
                />
              )}
              {!previewSrc && (
                <div className="h-[520px] flex flex-col gap-4 rounded bg-[var(--color-fill-2)] border border-[var(--color-fill-2)] justify-center items-center shadow shadow-[var(--color-fill-2)]">
                  <IconEyeInvisible className="w-12 h-12" />
                  <Typography.Paragraph>
                    {t['empty.preview']}
                  </Typography.Paragraph>
                </div>
              )}
            </Grid.Col>
          </Grid.Row>
          {type === 'web' && (
            <>
              <Divider style={{ margin: 0 }}>{t['config.other']}</Divider>
              {/* <WebSlotsConfigFormItem /> */}
              <VariableConfigFormItem />
            </>
          )}
        </Form>
      </Modal>
    </>
  );
};

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

const BUILTIN_VARIABLES = ['username', 'tenant', 'status', 'email', 'avatar'];

interface VariableConfigFormItemProps {}

const VariableConfigFormItem: React.FC<VariableConfigFormItemProps> = () => {
  const t = useLocale(i18n);
  const rules = useRules();
  return (
    <Form.Item label={t['config.inject.variables']}>
      <Form.List field="variables">
        {(fields, { add, remove }) => (
          <div className={styles.resetSlotContainer}>
            {fields.map((field) => (
              <div key={field.key} className={styles.resetSlotItemContainer}>
                <Form.Item
                  label={t['config.inject.variables.title']}
                  field={field.field + 'key'}
                  style={{ width: 400 }}
                  rules={rules}
                >
                  <Select placeholder={t['config.inject.variables.title']}>
                    {BUILTIN_VARIABLES.map((k) => (
                      <Select.Option key={k} value={k}>
                        {k}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item
                  field={field.field + 'type'}
                  triggerPropName="activeTab"
                >
                  <Tabs size="small">
                    <Tabs.TabPane
                      key={VariableInjectType.SET}
                      title={t['config.inject.set']}
                    >
                      <Form.Item
                        label={t['config.inject.set.body']}
                        field={field.field + 'body'}
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
                        field={field.field + 'body'}
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
                        field={field.field + 'body'}
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
                        field={field.field + 'body'}
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
                <div className={styles.resetSlotItemRemove}>
                  <Button
                    size="mini"
                    type="secondary"
                    icon={<IconDelete />}
                    status="danger"
                    onClick={() => remove(field.key)}
                  />
                </div>
              </div>
            ))}
            <Button
              long
              type="outline"
              status="success"
              icon={<IconPlus />}
              onClick={() =>
                add({
                  type: VariableInjectType.SET,
                })
              }
            >
              {t['config.reset.slot.add']}
            </Button>
          </div>
        )}
      </Form.List>
    </Form.Item>
  );
};

export default BotSettingsModal;
