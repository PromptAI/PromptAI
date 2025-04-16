import View, { ViewProps } from '../components/View';
import { NodeProps, NodeRule } from '../types';
import React, { useCallback, useMemo } from 'react';
import { getNodeLabel, unwrap } from '../utils/node';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import { VscDebugBreakpointConditional } from 'react-icons/vsc';
import DeleteNodeTrigger from '../components/DeleteNodeTrigger';
import { isEmpty, keyBy } from 'lodash';
import { CreateUserDrawerTrigger } from '../user';
import { CreateBotDrawerTrigger, initialBotValues } from '../bot';
import { useNodeDrop } from '../../dnd';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { useGraphStore } from '../../store/graph';
import { updateComponent } from '@/api/components';
import ru, { RU_TYPE } from '../../features/ru';
import { Button, Form, Input } from '@arco-design/web-react';
import ColorInput from '../components/ColorInput';
import Visible from '@/pages/projects/project/components/Visible';
import common from '../i18n';
import { IconEdit } from '@arco-design/web-react/icon';

export const ConditionIcon = VscDebugBreakpointConditional;

export interface ConditionViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
  rules: NodeRule[];
}
export const ConditionView: React.FC<ConditionViewProps> = ({
  node,
  rules,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<ConditionIcon />}
      id={node.id}
      label={label}
      color={node.data.color}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const ConditionNode: React.FC<NodeProps> = (props) => {
  const rules = useMemo<NodeRule[]>(() => {
    const emptyChildren = isEmpty(props.children);
    return [
      {
        key: 'user',
        show: emptyChildren,
      },
      { key: 'bot', show: emptyChildren },
    ];
  }, [props]);
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);
  const showDivider = useMemo(
    () => Boolean(rules.map((r) => r.show).filter(Boolean).length),
    [rules]
  );
  return (
    <MenuBox
      trigger={<ConditionView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateConditionDrawerTrigger node={props} />
      <DeleteNodeTrigger node={props} />
      {showDivider && <MenuBoxDivider />}
      {ruleMap['user'].show && <CreateUserDrawerTrigger parent={props} />}
      {ruleMap['bot'].show && (
        <CreateBotDrawerTrigger
          parent={props}
          initialValues={initialBotValues}
        />
      )}
    </MenuBox>
  );
};

export interface ConditionDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any> | (() => Partial<any>);
}
export const ConditionDrawerTrigger: React.FC<ConditionDrawerTriggerProps> = ({
  node,
  mode,
  initialValues: originInitialValues,
  ...props
}) => {
  const t = useLocale(i18n);
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));

  const initialValues = useMemo(() => {
    if (mode === 'update') return node.data;
    if (typeof originInitialValues === 'function') return originInitialValues();
    return originInitialValues;
  }, [mode, node.data, originInitialValues]);

  const onFinish = useCallback(
    async (data) => {
      if (mode === 'update') {
        const after = await updateComponent(projectId, 'break', node.id, {
          ...unwrap(node),
          data,
        });
        ru.push({
          type: RU_TYPE.UPDATE_NODE,
          changed: { after, before: unwrap(node) },
          dependencies: { flowId, projectId },
        });
      }
    },
    [flowId, mode, node, projectId]
  );
  return (
    <DrawerFormBoxTrigger
      title={t['title']}
      initialValues={initialValues}
      onFinish={onFinish}
      mode={mode}
      {...props}
    >
      <Form.Item
        label={t['form.name']}
        field="name"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <Input placeholder={t['form.name']} />
      </Form.Item>
      <Form.Item label={t['form.color']} field="color">
        <ColorInput />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export const UpdateConditionDrawerTrigger: React.FC<
  Omit<ConditionDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <ConditionDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
