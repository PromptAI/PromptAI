import Token from '@/utils/token';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  FormInstance,
  Input,
  Link,
  Message,
  Modal,
  Space,
  Upload,
} from '@arco-design/web-react';
import {
  IconClose,
  IconMenu,
  IconPlus,
  IconQuestionCircle,
} from '@arco-design/web-react/icon';
import { nanoid } from 'nanoid';
import React, { useRef, useState } from 'react';
import { encodeAttachmentText } from '@/utils/attachment';
import DraggableList from './draggable-list';
import i18n from './locale';
import { ActionProps, BotResponseFormItemProps } from './types';
import useConfig from './useConfig';
import CodeEditor from '@/components/CodeEditor';
import useRules from '@/hooks/useRules';
import useDocumentLinks from '@/hooks/useDocumentLinks';

const { Item } = Form;

const defaultValue = `# don't edit this function\`s name
async def run(self, 
    dispatcher, 
    tracker: Tracker,
    domain: Dict[Text, Any],) -> List[Dict[Text, Any]]:

    # dispatcher.utter_message(text="your slot is"+ tracker.get_slot("\${slotName}"))
    
    return []`;
const useModolAction = () => {
  const formRef = useRef<FormInstance>();
  const [props, setProps] = useState<ActionProps>({
    visible: false,
    action: () => void 0,
  });
  return [formRef, props, setProps] as const;
};
const BotResponseFormItem = ({
  fields,
  operation: { add, remove, move },
  responses,
  config: params,
  disabled,
  isFaq,
}: BotResponseFormItemProps) => {
  const config = useConfig(params, isFaq);
  const t = useLocale(i18n);
  const rules = useRules();
  const [attachmentFormRef, attachmentProps, setAttachmentProps] =
    useModolAction();
  const handleAttachmentOK = async () => {
    const { attachRess } = await attachmentFormRef.current.validate();
    if (attachRess && attachRess.length > 0) {
      const { response, status, name } = attachRess[0];
      if (status === 'done') {
        const { id } = response;
        const href = `/api/blobs/get/${id}`;
        const text = encodeAttachmentText({
          name,
          type: name?.split('.').pop() || 'other',
          href,
          version: '0.0.1',
        });
        attachmentProps.action(text);
        setAttachmentProps({ visible: false, action: () => void 0 });
        return;
      }
    }
    attachmentFormRef.current.setFields({
      attachRess: {
        value: attachRess,
        error: { message: t['conversation.botForm.rule.attachment'] },
      },
    });
  };
  const [actionFormRef, actionProps, setActionProps] = useModolAction();
  const handleActionOK = async () => {
    const { text, code } = await actionFormRef.current.validate();
    actionProps.action({ text, code });
    setActionProps({ visible: false, action: () => void 0 });
  };
  const docs = useDocumentLinks();

  return (
    <>
      <DraggableList allowedAnchor="draggable-allow-anchor" onChange={move}>
        {fields.map((field, index) => {
          const res = responses?.[index];
          if (!res) return null;
          const Component = config[res.type]?.component;
          if (!Component) return null;
          return (
            <div key={field.key} className="flex items-start gap-2 my-2">
              <Item
                key={res.id}
                field={field.field}
                rules={config[res.type].rules}
                className="!m-0 w-full"
              >
                <Component key={res.id} disabled={disabled} />
              </Item>
              {!disabled && (
                <div className="flex flex-col justify-between">
                  <Button
                    size="mini"
                    shape="circle"
                    status="danger"
                    type="text"
                    onClick={() => remove(index)}
                  >
                    <IconClose />
                  </Button>
                  <div className="px-2 cursor-move text-center draggable-allow-anchor">
                    <IconMenu />
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </DraggableList>
      {!disabled && (
        <>
          <Space size="mini" className="flex-wrap gap-2">
            {config.text && (
              <Button
                type="secondary"
                size="small"
                icon={<IconPlus />}
                onClick={() =>
                  add({
                    id: nanoid(),
                    type: 'text',
                    content: { text: '' },
                    delay: 500,
                  })
                }
              >
                {t['conversation.botForm.text']}
              </Button>
            )}
            {config.image && (
              <Button
                type="secondary"
                size="small"
                icon={<IconPlus />}
                onClick={() =>
                  add({
                    id: nanoid(),
                    type: 'image',
                    content: {
                      text: null,
                      image: [{ id: nanoid(), url: '' }],
                    },
                    delay: 500,
                  })
                }
              >
                {t['conversation.botForm.image']}
              </Button>
            )}
            {config.attachment && (
              <Button
                type="secondary"
                size="small"
                icon={<IconPlus />}
                onClick={() => {
                  setAttachmentProps({
                    visible: true,
                    action: (text) =>
                      add({
                        id: nanoid(),
                        type: 'attachment',
                        content: { text },
                        delay: 500,
                      }),
                  });
                }}
              >
                {t['conversation.botForm.attachment']}
              </Button>
            )}
            {config.webhook && (
              <Button
                type="secondary"
                size="small"
                icon={<IconPlus />}
                onClick={() =>
                  add({
                    id: nanoid(),
                    type: 'webhook',
                    content: { text: null, url: null, description: null },
                    delay: 500,
                  })
                }
              >
                {t['conversation.botForm.webhook']}
              </Button>
            )}
            {config.action && (
              <Button
                type="secondary"
                size="small"
                icon={<IconPlus />}
                onClick={() => {
                  setActionProps({
                    visible: true,
                    action: ({ text, code }) => {
                      add({
                        id: nanoid(),
                        type: 'action',
                        content: { text, code },
                        delay: 500,
                      });
                    },
                  });
                }}
              >
                {t['conversation.botForm.action']}
              </Button>
            )}
          </Space>
          <Modal
            title={t['conversation.botForm.attachment.upload.title']}
            visible={attachmentProps.visible}
            onCancel={() =>
              setAttachmentProps({ visible: false, action: () => void 0 })
            }
            onOk={handleAttachmentOK}
            unmountOnExit
          >
            <Form layout="vertical" ref={attachmentFormRef}>
              <Form.Item
                label={t['conversation.botForm.attachment']}
                field="attachRess"
              >
                <Upload
                  action="/api/blobs/upload"
                  limit={1}
                  accept="*"
                  headers={{ Authorization: Token.get() }}
                  multiple
                  beforeUpload={(file) => {
                    if (file.size > 20 * 1024 * 1024) {
                      Message.warning(
                        t['conversation.botForm.attachment.upload.filesize']
                      );
                      return Promise.reject();
                    }
                    return Promise.resolve();
                  }}
                  onExceedLimit={() => {
                    Message.warning(
                      t['conversation.botForm.attachment.upload.limit']
                    );
                  }}
                >
                  <div className="app-upload-drag-trigger">
                    <div>
                      {
                        t[
                          'conversation.botForm.attachment.upload.trigger.prefix'
                        ]
                      }
                      <span style={{ color: '#3370FF', padding: '0 4px' }}>
                        {
                          t[
                            'conversation.botForm.attachment.upload.trigger.suffix'
                          ]
                        }
                      </span>
                    </div>
                  </div>
                </Upload>
              </Form.Item>
            </Form>
          </Modal>
          <Modal
            style={{ width: '60%' }}
            title={t['conversation.botForm.action']}
            visible={actionProps.visible}
            onCancel={() =>
              setActionProps({ visible: false, action: () => void 0 })
            }
            onOk={handleActionOK}
            unmountOnExit
          >
            <Form
              layout="vertical"
              ref={actionFormRef}
              initialValues={{ code: defaultValue, text: 'action' }}
            >
              <Form.Item
                label={t['conversation.botForm.action.text']}
                field="text"
                extra={t['conversation.botForm.action.text.help']}
              >
                <Input
                  placeholder={
                    t['conversation.botForm.action.text.placeholder']
                  }
                />
              </Form.Item>
              <Form.Item
                label={
                  <Space>
                    {t['conversation.botForm.action.code']}
                    <Link target="_blank" href={docs.botAction}>
                      <IconQuestionCircle />
                    </Link>
                  </Space>
                }
                field="code"
                rules={rules}
              >
                <CodeEditor
                  width="100%"
                  height={340}
                  language="python"
                  options={{ minimap: { enabled: false } }}
                />
              </Form.Item>
            </Form>
          </Modal>
        </>
      )}
    </>
  );
};

export default BotResponseFormItem;
