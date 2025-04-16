import useRules from '@/hooks/useRules';
import useLocale, { useDefaultLocale } from '@/utils/useLocale';
import { Button, Card, Divider, Form, Grid, Input, Link, Message, Radio, Space } from '@arco-design/web-react';
import React, { useEffect, useRef, useState } from 'react';
import i18n from '@/pages/projects/locale';
import { Pairs } from '@/components/Pair';
import { IconCheckCircle, IconCloseCircle, IconCloud, IconQuestionCircle } from '@arco-design/web-react/icon';
import { useHistory, useParams } from 'react-router';
import { createWebhook } from '@/api/components';
import CustomInput from '../CustomInput';
import { useMount, useSafeState } from 'ahooks';
import { uniqueId } from 'lodash';
import useJsonRules from '../useJsonRules';
import useDocumentLinks from '@/hooks/useDocumentLinks';
import useMapRules from '@/pages/projects/project/webhooks/useMapRules';
import MonacoEditor from 'react-monaco-editor';
import CodeEditor from '@/components/CodeEditor';
import { DEFAULT_CODE } from '@/utils/constant';

const CreateWebhook = () => {
  const t = useLocale(i18n);
  const dt = useDefaultLocale();

  const jsonRules = useJsonRules();
  const mapRules = useMapRules();
  const history = useHistory();
  const { id: projectId } = useParams<{ id: string }>();

  const rules = useRules();

  const [form] = Form.useForm();

  const [responseType, setResponseType] = useState('not');
  const [webhookType, setWebhookType] = useState('web_request');

  const [requestHeaderType, setRequestHeaderType] = useState('none');

  const [submitting, setSubmitting] = useSafeState(false);

  useMount(() => {
    const webhook = JSON.parse(window.localStorage.getItem('clone_webhook'));
    if (webhook) {
      let agreement = null;
      let api = null;
      if (webhook.url.startsWith('http://')) {
        agreement = webhook.url.slice(0, 7);
        api = webhook.url.slice(7);
      }

      if (webhook.url.startsWith('https://')) {
        agreement = webhook.url.slice(0, 8);
        api = webhook.url.slice(8);
      }

      form.setFieldsValue({
        ...webhook,
        text: `${webhook.text}(copy ${uniqueId()})`,
        url: {
          agreement,
          api
        }
      });
    }
  });

  function handleRequestBody(values: any) {
    const { request_body_type, request_body } = values;

    // 请求 body 为 form 时，这里的 value是一个数组，需要将其转化为对象
    if (request_body_type === 'multipart/form-data' && request_body && request_body.length > 0) {
      return request_body.reduce((a, c) => ({ ...a, [c.key]: c.value }), {});
    }

    return request_body;
  }

  const onOk = () => {
    form
      .validate()
      .then((values) => {
        setSubmitting(true);
        if (webhookType === 'python_code') {
          return createWebhook(projectId, {
            ...values,
            text: extractFunctionName(code),
            data: {
              webhookType: webhookType,
              code: code
            }
          });
        }
        // web request
        return createWebhook(projectId, {
          ...values,
          data: {
            webhookType: webhookType
          },
          request_body: handleRequestBody(values),
          url: values.url.agreement + values.url.api,
          headers:
            values?.request_header_type === 'none' ? null : values?.headers,
          response_handle:
            values?.response_type !== 'custom'
              ? {
                parse: [],
                text: values?.response_handle?.text,
                error_msg: values?.response_handle?.error_msg
              }
              : values?.response_handle
        });
      })
      .then(() => {
        Message.success(dt['message.create.success']);
        window.localStorage.removeItem('clone_webhook');
        history.goBack();
      })
      .catch((error) => {
        console.error('Failed to create webhook:', error);
        Message.error(dt['message.create.failure']);
      })
      .finally(() => {
        setSubmitting(false);
      });
  };

  const onCancel = () => {
    window.localStorage.removeItem('clone_webhook');
    history.goBack();
  };

  const onValuesChange = (values) => {
    if (values.response_type) {
      setResponseType(values.response_type);
    }
    if (values.request_header_type) {
      setRequestHeaderType(values.request_header_type);
    }
    if (values.webhook_type) {
      setWebhookType(values.webhook_type);
    }
  };

  const docs = useDocumentLinks();

  /**
   * 将名称中的空格转换为下划线，提高输入体验，不用手动来回替换空格
   * @param value
   */
  const handleNameChange = (value: string) => {
    const formattedValue = value.replace(/\s/g, '_');
    form.setFieldValue('text', formattedValue);
  };

  const extractFunctionName = (code) => {
    const match = code.match(/def\s+(\w+)\s*\(/);
    return match ? match[1] : 'unknown_function';
  };

  const defaultCode = DEFAULT_CODE;
  const [code, setCode] = useState(defaultCode);

  // 处理 code 变化，设置 code、webhook name
  const handleEditorChange = (value) => {
    setCode(value); // 更新 Python 代码
  };

  function WebRequestView() {
    return (
      <>
        <Form.Item
          label={t['webhooks.form.name']}
          field="text"
          required={true}
          validateTrigger={['onChange', 'onBlur']}  // 添加这一行
          rules={[
            {
              validator: async (value, callback) => {
                const regex = /^[a-zA-Z0-9_-]+$/; // 正则表达式

                // 空值检查
                if (!value) {
                  callback(t['rule.required']);
                  return;
                }

                // 正则检查
                if (!regex.test(value)) {
                  callback(t['nameRule']);
                  return;
                }

                // 验证通过
                callback(); // 继续验证
              }
            }
          ]}
        >
          <Input onChange={handleNameChange} allowClear />
        </Form.Item>
        <Form.Item
          label={t['webhooks.form.url']}
          field="url"
          rules={[
            ...rules,

            {
              validator: (val, cb) => {
                if (!val.agreement) {
                  cb(t['webHook.from.url.select']);
                }
                if (!val.api) {
                  cb(t['webHook.from.empty']);
                }

                cb();
              }
            }
          ]}
        >
          <CustomInput />
        </Form.Item>
        <Form.Item
          label={t['webhooks.form.description']}
          field="description"
        >
          <Input.TextArea
            allowClear={true}
            autoSize={{ minRows: 2, maxRows: 6 }}
          />
        </Form.Item>
        <Divider orientation="center" style={{ margin: 0 }}>
          {t['webhooks.form.request.settings']}{' '}
          <a href={docs.reqeustSettings} target="_blank" rel="noreferrer">
            <IconQuestionCircle />
          </a>
        </Divider>
        <Form.Item
          label={t['webhooks.form.request-type']}
          field="request_type"
        >
          <Radio.Group>
            <Radio value="get">Get</Radio>
            <Radio value="post">Post</Radio>
            <Radio value="put">Put</Radio>
            <Radio value="delete">Delete</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item
          label={t['webHook.from.Header']}
          field="request_header_type"
        >
          <Radio.Group>
            <Radio value="none">{t['webHook.from.url.notHeader']}</Radio>
            <Radio value="custom">
              {t['webHook.from.url.customHeader']}
            </Radio>
          </Radio.Group>
        </Form.Item>
        {requestHeaderType === 'custom' && (
          <Form.Item
            field="headers"
            style={{ marginTop: -8 }}
            rules={[
              {
                validator(value, callback) {
                  const every = value?.every((value) => {
                    return !!value.key && !!value.value;
                  });
                  if (!every) {
                    callback(t['webHook.from.empty']);
                  }
                  callback();
                }
              }
            ]}
          >
            <Pairs
              keyName={t['webHook.from.header.pairs.key']}
              valueName={t['webHook.from.header.pairs.val']}
            />
          </Form.Item>
        )}
        <Form.Item
          label={t['webHook.from.request_body_type']}
          field="request_body_type"
        >
          <Radio.Group>
            <Radio value="none">
              {t['webHook.from.request_body_type.none']}
            </Radio>
            <Radio value="multipart/form-data">
              {t['webHook.from.request_body_type.from']}
            </Radio>
            <Radio value="application/json">
              {t['webHook.from.request_body_type.json']}
            </Radio>
            <Radio value="text/plain">
              {t['webHook.from.request_body_type.text']}
            </Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item shouldUpdate noStyle>
          {(values) => {
            switch (values.request_body_type) {
              case 'multipart/form-data':
                return (
                  <Form.Item
                    field="request_body"
                    style={{ marginTop: -8 }}
                    rules={mapRules}
                  >
                    <Pairs keyName="key" valueName="value" />
                  </Form.Item>
                );
              case 'application/json':
                return (
                  <Form.Item
                    field="request_body"
                    style={{ marginTop: -8 }}
                    rules={jsonRules}
                  >
                    <Input.TextArea
                      autoSize={{ minRows: 2, maxRows: 6 }}
                      placeholder={
                        t['webHook.from.request_body_type.json.placeholder']
                      }
                      allowClear={true}
                    />
                  </Form.Item>
                );
              case 'text/plain':
                return (
                  <Form.Item field="request_body" style={{ marginTop: -8 }}>
                    <Input.TextArea
                      autoSize={{ minRows: 2, maxRows: 6 }}
                      placeholder={
                        t['webHook.from.request_body_type.text.placeholder']
                      }
                      allowClear={true}
                    />
                  </Form.Item>
                );
              default:
                return <></>;
            }
          }}
        </Form.Item>
        <Divider orientation="center" style={{ margin: 0 }}>
          {t['webhooks.form.response.settings']}{' '}
          <a href={docs.responseSettings} target="_blank" rel="noreferrer">
            <IconQuestionCircle />
          </a>
        </Divider>
        <Form.Item
          label={t['webhooks.form.response-type']}
          field="response_type"
        >
          <Radio.Group>
            <Radio value="ignore_response">
              {t['webhooks.form.response-type.option.not']}
            </Radio>
            <Radio value="origin_response">
              {t['webhooks.form.response-type.option.direct']}
            </Radio>
            <Radio value="custom">
              {t['webhooks.form.response-type.option.custom']}
            </Radio>
          </Radio.Group>
        </Form.Item>
        {responseType === 'custom' && (
          <>
            <Form.Item
              label={
                <Space>
                  {t['webhooks.form.response-handle.parse']}
                  <span style={{ fontSize: 12 }}>
                        {t['webHook.from.response_body.placeholder']}
                    <Link
                      href="https://github.com/json-path/JsonPath"
                      target="_blank"
                      style={{ fontSize: 12 }}
                    >
                          {t['webHook.from.response_body.url']}
                        </Link>
                    {t['webHook.from.response_body.content.placeholder']}
                      </span>
                </Space>
              }
              field="response_handle.parse"
            >
              <Pairs
                showLabel
                keyName={t['webHook.from.response_body.pairs.key']}
                valueName={t['webHook.from.response_body.pairs.val']}
                spliter="->"
                reversed
              />
            </Form.Item>
            <Form.Item
              label={t['webhooks.form.response-handle.success']}
              field="response_handle.text"
              rules={rules}
            >
              <Input
                prefix={
                  <IconCheckCircle
                    style={{ color: 'rgb(var(--success-6))' }}
                  />
                }
                placeholder={
                  t['webhooks.form.response-handle.success.help']
                }
              />
            </Form.Item>
          </>
        )}
        {responseType === 'ignore_http_code' && (
          <Form.Item
            label={t['webhooks.form.response-handle.success']}
            field="response_handle.text"
            rules={rules}
          >
            <Input
              prefix={
                <IconCheckCircle
                  style={{ color: 'rgb(var(--success-6))' }}
                />
              }
              placeholder={t['webhooks.form.response-handle.success.help']}
            />
          </Form.Item>
        )}
        <Form.Item
          label={t['webhooks.form.response-handle.error']}
          field="response_handle.error_msg"
          rules={rules}
        >
          <Input
            prefix={
              <IconCloseCircle style={{ color: 'rgb(var(--danger-6))' }} />
            }
            placeholder={t['webhooks.form.response-handle.error.help']}
          />
        </Form.Item>
      </>
    );
  }

  return (
    <Card
      title={
        <Space>
          <IconCloud />
          {t['webhooks.create']}
        </Space>
      }
      bordered={false}
      size="small"
    >
      <Grid.Row justify="center">
        <Grid.Col offset={6} span={18}>
          <Form
            form={form}
            layout="vertical"
            onValuesChange={onValuesChange}
            style={{ width: 720 }}
            initialValues={{
              request_type: 'get',
              response_type: 'ignore_response',
              request_header_type: 'none',
              request_data_type: 'none',
              request_body_type: 'none',
              webhook_type: 'web_request',
              url: {
                agreement: 'http://'
              }
            }}
          >

            <Form.Item
              label={t['webhooks.form.webhook_type']}
              field="webhook_type"
            >
              <Radio.Group>
                <Radio value="web_request">Web Request</Radio>
                <Radio value="python_code">Python Code</Radio>
              </Radio.Group>
            </Form.Item>

            {webhookType === 'python_code' && (
              <div>
                <Form.Item
                  label={t['webhooks.form.description']}
                  field="description"
                >
                  <Input.TextArea
                    allowClear={true}
                    autoSize={{ minRows: 2, maxRows: 6 }}
                  />
                </Form.Item>
                <Form.Item label="Python Code" field="code">
                  <CodeEditor
                    className={'border'}
                    value={code}
                    onChange={handleEditorChange}
                    width="100%"
                    theme={'vs-dark'}
                    height={500}
                    language="python"
                    defaultValue={defaultCode}
                    options={{ minimap: { enabled: false } }}
                  />
                </Form.Item>
              </div>
            )}
            {webhookType === 'web_request' && <WebRequestView />}
            <Form.Item>
              <Space size="large">
                <Button type="secondary" onClick={onCancel}>
                  {t['webhooks.form.cancel']}
                </Button>
                <Button type="primary" loading={submitting} onClick={onOk}>
                  {t['webhooks.form.submit']}
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Grid.Col>
      </Grid.Row>
    </Card>
  );
};

export default CreateWebhook;
