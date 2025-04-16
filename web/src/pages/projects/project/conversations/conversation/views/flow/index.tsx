import React, { useCallback, useMemo } from 'react';
import { IconEdit, IconHome } from '@arco-design/web-react/icon';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import { Button, Form, Input } from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import TextArea from '@/pages/projects/project/components/TextArea';
import common from '../i18n';
import { updateComponent } from '@/api/components';
import { useGraphStore } from '../../store/graph';
import { getNodeLabel, unwrap } from '../utils/node';
import Visible from '@/pages/projects/project/components/Visible';
import { CreateBotDrawerTrigger, initialBotValues } from '../bot';
import { useProjectType } from '@/layout/project-layout/context';
import {CreateGptDrawerTrigger} from "../gpt/gpt";

export const FlowIcon = IconHome;

export const FlowView: React.FC<Omit<ViewProps, 'icon'>> = (props) => {
  return (
    <View
      icon={<FlowIcon />}
      validatorError={props.validatorError}
      {...props}
      firstComponent={props.firstComponent}
    />
  );
};

export const FlowNode: React.FC<NodeProps> = (props) => {
  const label = useMemo(() => getNodeLabel(props), [props]);
  const type = useProjectType();
  return (
    <MenuBox
      trigger={<FlowView id={props.id} label={label} firstComponent={props.first}/>}
      validatorError={props.validatorError}
    >
      <UpdateFlowDrawerTrigger node={props} />
      <MenuBoxDivider />
      <CreateBotDrawerTrigger parent={props} initialValues={initialBotValues} />
      {type === 'llm' && <CreateGptDrawerTrigger parent={props} />}
    </MenuBox>
  );
};

export interface FlowDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
}
export const FlowDrawerTrigger: React.FC<FlowDrawerTriggerProps> = ({
  node,
  ...props
}) => {
  const projectType = useProjectType();
  const projectId = useGraphStore((s) => s.projectId);
  const t = useLocale(i18n);
  const onFinish = useCallback(
    async (data) => {
      await updateComponent(projectId, 'conversation', node.id, {
        ...unwrap(node),
        data,
      });
    },
    [node, projectId]
  );

  return (
    <DrawerFormBoxTrigger
      title={t['title']}
      initialValues={node.data}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item
        label={t['form.name']}
        field="name"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <Input placeholder={t['form.name']} />
      </Form.Item>
      <Form.Item label={t['form.description']} field="description">
        <TextArea placeholder={t['form.description']} />
      </Form.Item>
      {/*{projectType === 'rasa' && (*/}
      {/*  <Form.Item label={t['form.welcome']} field="welcome">*/}
      {/*    <TextArea placeholder={t['form.welcome']} />*/}
      {/*  </Form.Item>*/}
      {/*)}*/}
    </DrawerFormBoxTrigger>
  );
};

export const UpdateFlowDrawerTrigger: React.FC<
  Omit<FlowDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <FlowDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
