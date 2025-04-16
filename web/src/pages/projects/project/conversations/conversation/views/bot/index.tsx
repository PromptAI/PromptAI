import React, { useCallback, useMemo, useRef, useState } from 'react';
import { IconEdit, IconSend, IconStar } from '@arco-design/web-react/icon';
import View, { ViewProps } from '../components/View';
import { NodeProps, NodeRule } from '../types';
import useLocale from '@/utils/useLocale';
import common from '../i18n';
import MenuBox, { MenuBoxDivider } from '@/pages/projects/project/components/MenuBox';
import { isEmpty, keyBy } from 'lodash';
import { DrawerFormBoxTrigger, DrawerFormBoxTriggerProps } from '../../../../components/DrawerBox';
import { Button, Form, FormInstance, Input, RulesProps, Tag } from '@arco-design/web-react';
import { useGraphStore } from '../../store/graph';
import i18n from './i18n';
import { useRequest } from 'ahooks';
import { createComponent, listSlotComponent, updateComponent } from '@/api/components';
import CustomLableFormItem from '../components/CustomLabelFormItem';
import { BotResponseFormItem } from '@/pages/projects/project/components/BotResponseFormItem';
import { ResponsesExtraProps } from './components/ResponsesExtra';
import Visible from '@/pages/projects/project/components/Visible';
import TextArea from '@/pages/projects/project/components/TextArea';
import ConditionsFormItem from '@/pages/projects/project/components/ConditionsFormItem';
import SetSlotsFormItem from '@/pages/projects/project/components/SetSlotsFormItem';
import {
  computeParentForm,
  findGotoFilterTargets,
  getBreakCount,
  getNodeLabel,
  parseResponseOfCreate,
  unwrap
} from '../utils/node';
import ru, { RU_TYPE } from '../../features/ru';
import { CreateUserDrawerTrigger } from '../user';
import { nanoid } from 'nanoid';
import { BotData, BotResponseBaseContent } from '@/graph-next/type';
import LinkedFromExtra from '../components/LinkedFromTopExtra';
import DeleteNodeTrigger from '../components/DeleteNodeTrigger';
import { useNodeDrop } from '../../dnd';
import { CreateBreakDrawerTrigger } from '../break';
import randomColor from '@/utils/randomColor';
import { CreateGotoDrawerTrigger } from '../goto';
import { useProjectType } from '@/layout/project-layout/context';
import { CreateGptDrawerTrigger } from '../gpt/gpt';

export const BotIcon = IconSend;

export interface BotViewProps extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}
export const BotView: React.FC<BotViewProps> = ({ rules, node, ...props }) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  const dropProps = useNodeDrop(node, rules);
  return (
    <View
      icon={<BotIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      topExtra={
        node.linkedFrom && <LinkedFromExtra linkedFrom={node.linkedFrom} />
      }
      {...props}
      {...dropProps}
    />
  );
};

export const BotNode: React.FC<NodeProps> = (props) => {
  const type = useProjectType();
  const nodes = useGraphStore((s) =>
    s.nodes.filter((s) => !['conversation'].includes(s.type))
  );
  const rules = useMemo<NodeRule[]>(() => {
    const parentForm = computeParentForm(props);
    const emptyChildren = isEmpty(props.children);
    const filters = findGotoFilterTargets(props);
    const targets = nodes.filter((n) => !filters.some((f) => f.id === n.id));
    return [
      {
        key: 'user',
        show: !props.children?.some((n) => ['bot', 'break'].includes(n.type)),
      },
      { key: 'bot', show: emptyChildren },
      {
        key: 'goto',
        show: targets.length > 0 && emptyChildren,
      },
      { key: 'form', show: !parentForm && emptyChildren },
      { key: 'form-gpt', show: !parentForm && emptyChildren },
      { key: 'break', show: emptyChildren && parentForm },
      { key: 'gpt', show: emptyChildren },
    ];
  }, [props, nodes]);
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);
  const showDivider = useMemo(
    () => Boolean(rules.map((r) => r.show).filter(Boolean).length),
    [rules]
  );
  const initialValues = useCallback(() => {
    const form = computeParentForm(props);
    const breakCount = getBreakCount(form);
    return {
      name: `break-${breakCount + 1}`,
      color: randomColor(),
      formId: form.id,
    };
  }, [props]);

  return (
    <MenuBox
      trigger={<BotView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateBotDrawerTrigger node={props} />
      {/*<CopyNodeTrigger node={props} />*/}
      <DeleteNodeTrigger node={props} />
      <MenuBoxDivider />
      {ruleMap['user'].show && <CreateUserDrawerTrigger parent={props} />}
      {ruleMap['bot'].show && (
        <CreateBotDrawerTrigger
          parent={props}
          initialValues={initialBotValues}
        />
      )}
      {ruleMap['goto'].show && <CreateGotoDrawerTrigger parent={props} />}
      {/*{type === 'rasa' && ruleMap['form'].show && (*/}
      {/*  <CreateFormDrawerTrigger parent={props} />*/}
      {/*)}*/}
      {/*{type === 'llm' && ruleMap['form'].show && (*/}
      {/*  <CreateGptFormDrawerTrigger parent={props} />*/}
      {/*)}*/}
      {type === 'llm' && ruleMap['gpt'].show && <CreateGptDrawerTrigger parent={props} />}
      {ruleMap['break'].show && (
        <CreateBreakDrawerTrigger
          parent={props}
          initialValues={initialValues}
        />
      )}
      {showDivider && <MenuBoxDivider />}
       {/*<FavoriteNodeTrigger node={props} />*/}
    </MenuBox>
  );
};
interface BotDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any> | (() => Partial<any>);
}
const BotDrawerTrigger: React.FC<BotDrawerTriggerProps> = ({
  mode,
  node,
  initialValues: originInitialValues,
  ...props
}) => {
  const formRef = useRef<FormInstance>();
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));
  const t = useLocale(i18n);
  const responsesRules = useMemo<RulesProps[]>(
    () => [
      {
        required: true,
        minLength: 1,
        message: t['rule.responses'],
      },
    ],
    [t]
  );
  const initialValues = useMemo(() => {
    if (mode === 'update') return node.data;
    if (typeof originInitialValues === 'function') return originInitialValues();
    return originInitialValues;
  }, [mode, node.data, originInitialValues]);

  const [linkedFrom, setLinkedFrom] = useState(() =>
    mode === 'update' ? node.linkedFrom : null
  );
  const onLinkedFrom = useCallback<ResponsesExtraProps['onLinkedFrom']>(
    (data, linked) => {
      formRef.current.setFieldsValue(data);
      setLinkedFrom(linked);
    },
    []
  );
  const { loading, data: slots = [] } = useRequest(
    () => listSlotComponent(projectId),
    {
      refreshDeps: [projectId],
      manual: !props.visible,
    }
  );

  const replyConditionOptions = useMemo(
    () =>
      slots.map(({ id, name, display }) => ({
        label: display || name,
        value: id,
      })),
    [slots]
  );
  const onFinish = useCallback(
    async (data) => {
      if (mode === 'update') {
        const after = await updateComponent(projectId, 'bot', node.id, {
          ...unwrap(node),
          data,
          linkedFrom,
        });
        ru.push({
          type: RU_TYPE.UPDATE_NODE,
          changed: {
            after,
            before: unwrap(node),
          },
          dependencies: {
            flowId,
            projectId,
          },
        });
      }
      if (mode === 'create') {
        const nodes = await createComponent(projectId, 'bot', {
          data,
          parentId: node.id,
          linkedFrom,
        });
        const [changed, children] = parseResponseOfCreate(nodes, 'bot');
        ru.push({
          type: RU_TYPE.ADD_NODE,
          changed,
          dependencies: {
            flowId,
            projectId,
            children,
          },
        });
      }
    },
    [flowId, linkedFrom, mode, node, projectId]
  );
  return (
    <DrawerFormBoxTrigger
      ref={formRef}
      title={
        <div className="space-x-2">
          <span>{t['title']}</span>
          {linkedFrom && (
            <Tag size="small" color="orange" icon={<IconStar />}>
              {linkedFrom.name}
            </Tag>
          )}
        </div>
      }
      initialValues={initialValues}
      loading={loading}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item shouldUpdate noStyle>
        {({ responses }) => (
          <CustomLableFormItem
            // label={
            //   <ResponsesExtra
            //     label={t['form.responses']}
            //     nodeId={node.id}
            //     // linkedFrom={linkedFrom}
            //     // onLinkedFrom={onLinkedFrom}
            //     formRef={formRef}
            //     mode={mode}
            //   />
            // }
            label={t['form.responses']}
            field="responses"
            required
            rules={responsesRules}
          >
            <Form.List field="responses">
              {(fields, operation) => (
                <BotResponseFormItem
                  fields={fields}
                  operation={operation}
                  responses={responses}
                />
              )}
            </Form.List>
          </CustomLableFormItem>
        )}
      </Form.Item>
      {/*<Form.Item label={t['form.description']} field="description">*/}
      {/*  <TextArea placeholder={t['form.description']} />*/}
      {/*</Form.Item>*/}
      <Form.Item
        label={t['form.reply.condition']}
        field="conditions"
        extra={t['form.reply.condition.help']}
      >
        <Form.List field="conditions">
          {(fields, operation) => (
            <ConditionsFormItem
              fields={fields}
              operation={operation}
              slots={slots}
            />
          )}
        </Form.List>
      </Form.Item>
      <Form.Item
        label={t['form.reset.slot']}
        field="setSlots"
        extra={t['form.reset.slot.help']}
      >
        <Form.List field="setSlots">
          {(fields, operation) => (
            <SetSlotsFormItem
              fields={fields}
              operation={operation}
              slots={slots}
            />
          )}
        </Form.List>
      </Form.Item>
      {/*<Form.Item label={t['form.condition.responses']} field="entityReplies">*/}
      {/*  <ReplyVariables options={replyConditionOptions} />*/}
      {/*</Form.Item>*/}
      <Form.Item field="name" className="!hidden">
        <Input />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateBotDrawerTriggerProps
  extends Omit<BotDrawerTriggerProps, 'mode' | 'trigger' | 'node' | 'refresh'> {
  parent: Partial<NodeProps>;
}
export const CreateBotDrawerTrigger: React.FC<CreateBotDrawerTriggerProps> = ({
  parent,
  ...props
}) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <BotDrawerTrigger
        mode="create"
        node={parent}
        trigger={<Button icon={<BotIcon />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateBotDrawerTrigger: React.FC<
  Omit<BotDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <BotDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
export const initialBotValues = (): BotData<BotResponseBaseContent> => ({
  responses: [
    {
      id: nanoid(),
      type: 'text',
      content: {
        text: '',
      },
      delay: 500,
    },
  ],
});
