import {
  IconBranch,
  IconCloud,
  IconCode,
  IconDelete,
  IconEdit,
  IconOrderedList,
  IconPlus,
  IconQuestionCircle,
} from '@arco-design/web-react/icon';
import * as React from 'react';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import MenuBox from '../../../components/MenuBox';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../components/DrawerBox';
import { useGraphStore } from '../../store';
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
import i18n from './i18n';
import common from '../i18n/common';
import Visible from '../../../components/Visible';
import { nanoid } from 'nanoid';
import ActionSelector from '../../drawer/forms/components/ActionSelector';
import { useProjectContext } from '@/layout/project-layout/context';
import { Webhook } from '../../../components/BotResponseFormItem';
import useRules from '@/hooks/useRules';
import { updateProject } from '@/api/projects';
import styled from 'styled-components';
import useModalForm from '@/hooks/useModalForm';
import { debounce, intersectionBy, xorBy } from 'lodash';
import { getFaqList } from '@/api/faq';
import useUrlParams from '../../../hooks/useUrlParams';
import { useRequest } from 'ahooks';

export const FallbackIcon = IconQuestionCircle;

export interface FallbackViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
const nameFuncMap = {
  undefined: (data, builtInName) => data?.fallback || builtInName,
  text: (data, builtInName) => data?.fallback || builtInName,
  webhook: (data) => data?.webhooks?.[0]?.content.text || '-',
  action: (data) => data?.text || 'Action',
  action_promptai: (_, builtInName, defaultName) => defaultName,
  talk2bits: () => 'deprecated',
};
export const FallbackView: React.FC<FallbackViewProps> = ({
  node,
  ...props
}) => {
  const t = useLocale(i18n);
  const name = React.useMemo(() => {
    return nameFuncMap[node.data?.fallbackType](
      node.data,
      t['fallback.title.unknown'],
      t['fallback.title.action_promptai']
    );
  }, [node.data, t]);
  return (
    <View
      id={node.id}
      // {`${t[`fallback.form.type.${node.data?.fallbackType}`]}:${name}`}
      label={`${t[`fallback.title`]}: ${name}`}
      icon={<FallbackIcon />}
      {...props}
    />
  );
};
export const FallbackNode: React.FC<NodeProps> = (props) => {
  return (
    <MenuBox trigger={<FallbackView node={props} />}>
      <UpdateFallbackDrawerTrigger node={props} />
    </MenuBox>
  );
};

export interface FallbackDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  node: Partial<NodeProps>;
}
const getActions = (action) =>
  action ? [{ id: nanoid(), type: 'action', delay: 500, ...action }] : [];
type FallbackType = 'text' | 'webhook' | 'action' | 'action_promptai';

export const FallbackDrawerTrigger: React.FC<FallbackDrawerTriggerProps> = ({
  node,
  ...props
}) => {
  const t = useLocale(i18n);
  const initialValues = React.useMemo(
    () => ({
      formValues: {
        fallback: node.data.fallback,
        webhook: node.data.webhooks?.[0],
        action:
          node.data.fallbackType !== 'action_promptai'
            ? node.data.actions?.[0]
            : undefined,
        builtinAction:
          node.data.fallbackType === 'action_promptai'
            ? node.data.actions?.[0]
            : undefined,
        fallbackButtons: node.data.fallbackButtons,
      },
      initialType: node.data.fallbackType ?? 'text',
    }),
    [node.data]
  );
  const { refresh, ...project } = useProjectContext();
  const [fallbackType, setType] = React.useState<FallbackType>(
    initialValues.initialType || 'text'
  );
  const rules = useRules();
  const onFinish = React.useCallback(
    async (data) => {
      const { webhook, action, builtinAction, ...rest } = data;
      let type = 'text';
      if (webhook && fallbackType === 'webhook') {
        type = 'webhook';
      }
      if (action && fallbackType === 'action') {
        type = 'action';
      }
      await updateProject({
        ...project,
        ...rest,
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
    [fallbackType, project, refresh]
  );
  const conversationOptions = useGraphStore<FallbackButton[]>((s) =>
    s.nodes
      .filter((n) => n.type === 'conversation')
      .map((n) => ({
        id: n.id,
        text: n.data?.name || '-',
        type: n.type,
      }))
  );
  return (
    <DrawerFormBoxTrigger
      title={t['fallback.title']}
      initialValues={initialValues.formValues}
      onFinish={onFinish}
      {...props}
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
              {t['fallback.form.type.webhook']}
            </div>
          </Radio>
          <Radio value="action" style={{ marginRight: 8 }}>
            <div
              style={{
                display: 'inline',
              }}
            >
              <IconCode />
              {t['fallback.form.type.action']}
            </div>
          </Radio>
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
      <Form.Item label={t['fallback.form.text']} field="fallback" rules={rules}>
        <Input placeholder={t['fallback.form.text.placeholder']} />
      </Form.Item>
      <Form.Item
        label={t['fallback.form.fallbackButtons']}
        field="fallbackButtons"
      >
        <FallbackButtons conversationOptions={conversationOptions} />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};
export const UpdateFallbackDrawerTrigger: React.FC<
  Omit<FallbackDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <FallbackDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

type FallbackButton = {
  id: string;
  text: string;
  type: string;
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
  const conversations = React.useMemo(
    () => value?.filter((v) => v.type === 'conversation') || [],
    [value]
  );
  const [cVisible, setCVisible, cForm] = useModalForm();
  const conversationSelectbles = React.useMemo(
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
  const faqs = React.useMemo(
    () => value?.filter((v) => v.type === 'faq') || [],
    [value]
  );
  const [params, setParams] = React.useState<string>();
  const { loading, data: faqOptions = [] } = useRequest(
    () => fetchFAQs(projectId, params),
    {
      refreshDeps: [projectId, params],
    }
  );
  const onSearch = React.useMemo(
    () => debounce((v: string) => setParams(v), 500),
    []
  );
  const faqSelectbles = React.useMemo(
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
