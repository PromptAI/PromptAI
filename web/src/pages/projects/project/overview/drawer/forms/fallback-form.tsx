import { updateProject } from '@/api/projects';
import { GraphNode } from '@/graph-next/type';
import useFormRef from '@/hooks/useFormRef';
import useRules from '@/hooks/useRules';
import { useProjectContext } from '@/layout/project-layout/context';
import useLocale from '@/utils/useLocale';
import {
  Button,
  Form,
  Input,
  List,
  Modal,
  Radio,
  Select,
} from '@arco-design/web-react';
import React, { Ref, useImperativeHandle, useMemo, useState } from 'react';
import { SelectionProps } from '../../types';
import i18n from '../locale';
import { ComponentHandle } from '../type';
import { Webhook } from '@/pages/projects/project/components/BotResponseFormItem';
import {
  IconBranch,
  IconCloud,
  IconCode,
  IconDelete,
  IconOrderedList,
  IconPlus,
} from '@arco-design/web-react/icon';
import { nanoid } from 'nanoid';
import ActionSelector from './components/ActionSelector';
import { useRequest } from 'ahooks';
import styled from 'styled-components';
import useModalForm from '@/hooks/useModalForm';
import { debounce, intersectionBy, xorBy } from 'lodash';
import { getFaqList } from '@/api/faq';
import useUrlParams from '../../../hooks/useUrlParams';

type FallbackType = 'text' | 'webhook' | 'action' | 'action_promptai';
// | 'talk2bits';
interface Fallback extends GraphNode {
  data: {
    fallback: string;
    fallbackType?: FallbackType;
    webhooks?: any[];
    actions?: any[];
    // talk2bits?: string;
    fallbackButtons?: FallbackButton[];
  };
}
const getActions = (action) =>
  action ? [{ id: nanoid(), type: 'action', delay: 500, ...action }] : [];
const FallbackForm = (
  { selection, graph }: SelectionProps<Fallback>,
  ref: Ref<ComponentHandle>
) => {
  const initialValues = useMemo(
    () => ({
      formValues: {
        fallback: selection.data.fallback,
        webhook: selection.data.webhooks?.[0],
        action:
          selection.data.fallbackType !== 'action_promptai'
            ? selection.data.actions?.[0]
            : undefined,
        builtinAction:
          selection.data.fallbackType === 'action_promptai'
            ? selection.data.actions?.[0]
            : undefined,
        // talk2bits: selection.data.talk2bits,
        fallbackButtons: selection.data.fallbackButtons,
      },
      initialType: selection.data.fallbackType ?? 'text',
    }),
    [selection.data]
  );
  const formRef = useFormRef(initialValues.formValues);
  const rules = useRules();
  const t = useLocale(i18n);
  const { refresh, ...project } = useProjectContext();
  const [fallbackType, setType] = useState<FallbackType>(
    initialValues.initialType || 'text'
  );
  const conversationOptions = useMemo<FallbackButton[]>(() => {
    return graph.flatMap((g) =>
      g.type === 'conversation'
        ? [
            {
              id: g.id,
              text: g.data.name,
              type: g.type,
            },
          ]
        : []
    );
  }, [graph]);
  useImperativeHandle(
    ref,
    () => ({
      handle: async () => {
        // const { webhook, action, builtinAction, talk2bits, ...rest } =
        //   await formRef.current.validate();
        const { webhook, action, builtinAction, ...rest } =
          await formRef.current.validate();
        let type = 'text';
        if (webhook && fallbackType === 'webhook') {
          type = 'webhook';
        }
        if (action && fallbackType === 'action') {
          type = 'action';
        }
        // if (talk2bits && fallbackType === 'talk2bits') {
        //   type = 'talk2bits';
        // }
        await updateProject({
          ...project,
          ...rest,
          // talk2bits,
          fallbackType: type,
          webhooks: webhook
            ? [{ id: nanoid(), type: 'webhook', delay: 500, ...webhook }]
            : [],
          actions: getActions(
            type === 'action_promptai' ? builtinAction : action
          ),
        });
        refresh();
      },
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [project, fallbackType]
  );
  return (
    <Form
      layout="vertical"
      ref={formRef}
      initialValues={initialValues.formValues}
    >
      <Form.Item>
        <Radio.Group defaultValue={fallbackType} onChange={setType}>
          <Radio value="webhook" style={{ marginRight: 8 }}>
            <div
              style={{
                display: 'inline',
              }}
            >
              <IconCloud />
              {t['fallback.form.type.webhook']}{' '}
            </div>
          </Radio>
          <Radio value="action" style={{ marginRight: 8 }}>
            <div
              style={{
                display: 'inline',
              }}
            >
              <IconCode />
              {t['fallback.form.type.action']}{' '}
            </div>
          </Radio>
          {/* <Radio value="talk2bits">
            <div
              style={{
                display: 'inline',
              }}
            >
              <span
                style={{
                  width: 18,
                  height: 18,
                  background: 'url("/talk2bits.png") no-repeat center 2px',
                  backgroundPositionY: '4px',
                  backgroundSize: 'cover',
                  display: 'inline-block',
                }}
              />
              Talk2Bits{' '}
              <a
                href="https://talk2bits.com"
                target="_blank"
                rel="noreferrer"
                onClick={(evt) => evt.stopPropagation()}
                style={{ fontSize: 14 }}
              >
                talk2bits.com
              </a>
            </div>
          </Radio> */}
        </Radio.Group>
      </Form.Item>
      {fallbackType === 'webhook' && (
        <Form.Item field="webhook">
          <Webhook />
        </Form.Item>
      )}
      {fallbackType === 'action' && (
        <Form.Item label={t['fallback.form.action']} field="action">
          <ActionSelector placeholder={t['fallback.form.action.placeholder']} />
        </Form.Item>
      )}
      {/* {fallbackType === 'talk2bits' && (
        <Form.Item field="talk2bits">
          <Input.TextArea
            autoSize={{ minRows: 2 }}
            placeholder={t['fallback.form.tal2bits.placeholder']}
          />
        </Form.Item>
      )} */}
      <Form.Item label={t['fallback.form.text']} field="fallback" rules={rules}>
        <Input placeholder={t['fallback.form.text.placeholder']} />
      </Form.Item>
      <Form.Item
        label={t['fallback.form.fallbackButtons']}
        field="fallbackButtons"
      >
        <FallbackButtons conversationOptions={conversationOptions} />
      </Form.Item>
    </Form>
  );
};

export default FallbackForm;

type FallbackButton = {
  id: string;
  text: string;
  type: 'conversation' | 'faq';
};

interface FallbackButtonsProps {
  value?: FallbackButton[];
  onChange?: (value: FallbackButton[]) => void;
  conversationOptions: FallbackButton[];
}
const ListWrap = styled(List)`
  > .arco-list-footer {
    border: none;
    padding: 0;
  }
`;
const ListItem = styled(List.Item)`
  border: 1px solid var(--color-border-1);
  padding: 8px !important;
  border-radius: 4px;
  margin: 8px 0;
  .arco-list-item-content {
    height: 100%;
    display: flex;
    align-items: center;
    gap: 4px;
    padding-right: 8px;
  }
`;
const ListItemLabel = styled.div`
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;
const FallbackButtons: React.FC<FallbackButtonsProps> = ({
  conversationOptions,
  value,
  onChange,
}) => {
  const onDelete = (item: FallbackButton) => {
    onChange?.(value ? value.filter((v) => v.id !== item.id) : []);
  };
  return (
    <>
      <Conversations
        value={value}
        onChange={onChange}
        onDelete={onDelete}
        conversationOptions={conversationOptions}
      />
      <FAQs value={value} onChange={onChange} onDelete={onDelete} />
    </>
  );
};
interface ConversationsProps {
  value?: FallbackButton[];
  onChange?: (value: FallbackButton[]) => void;
  conversationOptions: FallbackButton[];
  onDelete: (item: FallbackButton) => void;
}
const Conversations: React.FC<ConversationsProps> = ({
  value,
  onChange,
  onDelete,
  conversationOptions,
}) => {
  const t = useLocale(i18n);
  const conversations = useMemo(
    () => value?.filter((v) => v.type === 'conversation') || [],
    [value]
  );
  const [cVisible, setCVisible, cForm] = useModalForm();
  const conversationSelectbles = useMemo(
    () =>
      value
        ? xorBy(
            intersectionBy(conversationOptions, value, 'id'),
            conversationOptions,
            'id'
          )
        : conversationOptions,
    [conversationOptions, value]
  );
  const onConversationOk = async () => {
    const { selectedId } = await cForm.validate();
    const selected = conversationSelectbles.find((c) => c.id === selectedId);
    if (selected) {
      onChange?.(value ? value.concat(selected) : [selected]);
    }
    setCVisible(false);
  };
  return (
    <>
      <Form.Item label={t['fallback.form.fallbackButtons.conversation']}>
        <ListWrap
          size="small"
          bordered={false}
          dataSource={conversations}
          noDataElement={<span></span>}
          render={(item) => (
            <ListItem
              key={item.id}
              actions={[
                <Button
                  size="mini"
                  type="secondary"
                  icon={<IconDelete />}
                  key="delete"
                  status="danger"
                  onClick={() => onDelete(item)}
                />,
              ]}
            >
              <IconBranch />
              <ListItemLabel>{item.text}</ListItemLabel>
            </ListItem>
          )}
          footer={
            <Button
              type="dashed"
              status="success"
              long
              icon={<IconPlus />}
              onClick={() => setCVisible(true)}
            >
              {t['fallback.form.fallbackButtons.add']}
            </Button>
          }
        />
      </Form.Item>
      <Modal
        title={t['fallback.form.fallbackButtons.modal.conversation.title']}
        visible={cVisible}
        unmountOnExit
        onCancel={() => setCVisible(false)}
        onOk={onConversationOk}
      >
        <Form layout="vertical" form={cForm}>
          <Form.Item
            label={
              t['fallback.form.fallbackButtons.modal.conversation.selectedId']
            }
            field="selectedId"
          >
            <Select
              placeholder={
                t[
                  'fallback.form.fallbackButtons.modal.conversation.selectedId.placeholder'
                ]
              }
            >
              {conversationSelectbles.map((item) => (
                <Select.Option key={item.id} value={item.id}>
                  {item.text}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
interface FAQsProps {
  value?: FallbackButton[];
  onChange?: (value: FallbackButton[]) => void;
  onDelete: (item: FallbackButton) => void;
}
const fetchFAQs = async (projectId: string, q?: string) => {
  const {
    data: { data },
  } = await getFaqList({ projectId, q, page: 0, size: 20 });
  const users: FallbackButton[] = data.map(
    ({
      user: {
        data: { examples },
        id,
      },
    }) => ({
      id,
      type: 'faq',
      text: examples[0]?.text || '-',
    })
  );
  return users;
};
const FAQs: React.FC<FAQsProps> = ({ value, onChange, onDelete }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [visible, setVisible, form] = useModalForm();
  const faqs = useMemo(
    () => value?.filter((v) => v.type === 'faq') || [],
    [value]
  );
  const [params, setParams] = useState<string>();
  const { loading, data: faqOptions = [] } = useRequest(
    () => fetchFAQs(projectId, params),
    {
      refreshDeps: [projectId, params],
    }
  );
  const onSearch = useMemo(
    () => debounce((v: string) => setParams(v), 500),
    []
  );
  const faqSelectbles = useMemo(
    () =>
      value
        ? xorBy(intersectionBy(faqOptions, value, 'id'), faqOptions, 'id')
        : faqOptions,
    [faqOptions, value]
  );
  const onOk = async () => {
    const { selectedId } = await form.validate();
    const selected = faqSelectbles.find((c) => c.id === selectedId);
    if (selected) {
      onChange?.(value ? value.concat(selected) : [selected]);
    }
    setVisible(false);
  };
  return (
    <>
      <Form.Item label={t['fallback.form.fallbackButtons.faq']}>
        <ListWrap
          size="small"
          bordered={false}
          dataSource={faqs}
          noDataElement={<span></span>}
          render={(item) => (
            <ListItem
              key={item.id}
              actions={[
                <Button
                  size="mini"
                  type="secondary"
                  icon={<IconDelete />}
                  key="delete"
                  status="danger"
                  onClick={() => onDelete(item)}
                />,
              ]}
            >
              <IconOrderedList />
              <ListItemLabel>{item.text}</ListItemLabel>
            </ListItem>
          )}
          footer={
            <Button
              type="dashed"
              status="success"
              long
              icon={<IconPlus />}
              onClick={() => setVisible(true)}
            >
              {t['fallback.form.fallbackButtons.add']}
            </Button>
          }
        />
      </Form.Item>
      <Modal
        title={t['fallback.form.fallbackButtons.modal.faq.title']}
        visible={visible}
        unmountOnExit
        onCancel={() => setVisible(false)}
        onOk={onOk}
      >
        <Form layout="vertical" form={form}>
          <Form.Item
            label={t['fallback.form.fallbackButtons.modal.faq.selectedId']}
            field="selectedId"
          >
            <Select
              placeholder={
                t[
                  'fallback.form.fallbackButtons.modal.faq.selectedId.placeholder'
                ]
              }
              loading={loading}
              showSearch
              onSearch={onSearch}
            >
              {faqSelectbles.map((item) => (
                <Select.Option key={item.id} value={item.id}>
                  {item.text}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
