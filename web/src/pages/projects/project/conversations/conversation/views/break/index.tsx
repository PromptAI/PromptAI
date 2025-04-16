import { VscDebugBreakpointLog } from 'react-icons/vsc';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import React, { useCallback, useMemo } from 'react';
import {
  computeParentForm,
  getNodeLabel,
  parseResponseOfCreate,
  unwrap,
} from '../utils/node';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import DeleteNodeTrigger from '../components/DeleteNodeTrigger';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { Button, Form, Input } from '@arco-design/web-react';
import ColorInput from '../components/ColorInput';
import Visible from '@/pages/projects/project/components/Visible';
import common from '../i18n';
import { IconEdit } from '@arco-design/web-react/icon';
import { createComponent, updateComponent } from '@/api/components';
import { useGraphStore } from '../../store/graph';
import ru, { RU_TYPE } from '../../features/ru';

export const BreakIcon = VscDebugBreakpointLog;

export interface BreakViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const BreakView: React.FC<BreakViewProps> = ({ node, ...props }) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  return (
    <View
      icon={<BreakIcon />}
      id={node.id}
      label={label}
      color={node.data?.color}
      validatorError={node.validatorError}
      {...props}
    />
  );
};

export const BreakNode: React.FC<NodeProps> = (props) => {
  return (
    <MenuBox
      trigger={<BreakView node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateBreakDrawerTrigger node={props} />
      <DeleteNodeTrigger node={props} />
    </MenuBox>
  );
};

export interface BreakDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
  initialValues?: Partial<any> | (() => Partial<any>);
}
export const BreakDrawerTrigger: React.FC<BreakDrawerTriggerProps> = ({
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
      if (mode === 'create') {
        const form = computeParentForm(node as any);
        const nodes = await createComponent(projectId, 'break', {
          data: {
            ...data,
            formId: form.id,
          },
          parentId: node.id,
        });
        const [changed, children] = parseResponseOfCreate(nodes, 'break');
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

export interface CreateBreakDrawerTriggerProps
  extends Omit<
    BreakDrawerTriggerProps,
    'mode' | 'trigger' | 'node' | 'refresh'
  > {
  parent: Partial<NodeProps>;
}
export const CreateBreakDrawerTrigger: React.FC<
  CreateBreakDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <BreakDrawerTrigger
        mode="create"
        node={parent}
        trigger={<Button icon={<BreakIcon />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateBreakDrawerTrigger: React.FC<
  Omit<BreakDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <BreakDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
