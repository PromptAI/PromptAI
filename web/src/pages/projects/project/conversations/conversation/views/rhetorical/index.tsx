import { IconEdit, IconQuestionCircle } from '@arco-design/web-react/icon';
import React, { useCallback, useMemo } from 'react';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import { getNodeLabel, unwrap } from '../utils/node';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import { CreateUserDrawerTrigger } from '../user';
import { isEmpty } from 'lodash';
import { IntentNextData } from '@/graph-next/type';
import { nanoid } from 'nanoid';
import { MappingTypeEnum } from '../user/enums';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import Visible from '@/pages/projects/project/components/Visible';
import { Button, Form } from '@arco-design/web-react';
import common from '../i18n';
import { Text } from '@/pages/projects/project/components/BotResponseFormItem';
import { updateComponent } from '@/api/components';
import { useGraphStore } from '../../store/graph';
import ru, { RU_TYPE } from '../../features/ru';

export const RhetoricalIcon = IconQuestionCircle;

export interface RhetoricalViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  node: NodeProps;
}
export const RhetoricalView: React.FC<RhetoricalViewProps> = ({
  node,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  return (
    <View
      icon={<RhetoricalIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
    />
  );
};

export const RhetoricalNode: React.FC<NodeProps> = (props) => {
  const showCreateUser = useMemo(
    () => !isEmpty(props.parent?.data?.slotId),
    [props.parent?.data?.slotId]
  );
  const initialValues = useCallback<() => Partial<IntentNextData>>(
    () => ({
      mappingsEnable: true,
      mappings: [
        {
          ...props.parent.data,
          id: nanoid(),
          type: MappingTypeEnum.FROM_ENTITY,
          enable: true,
        },
      ],
    }),
    [props.parent.data]
  );
  return (
    <MenuBox
      trigger={<RhetoricalView node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateRhetoricalDrawerTrigger node={props} />
      {showCreateUser && <MenuBoxDivider />}
      {showCreateUser && (
        <CreateUserDrawerTrigger
          parent={props}
          initialValues={initialValues}
          afterRhetorical={props.id}
        />
      )}
    </MenuBox>
  );
};
export interface RhetoricalDrawerTriggerProps
  extends DrawerFormBoxTriggerProps {
  node: Partial<NodeProps>;
}
export const RhetoricalDrawerTrigger: React.FC<
  RhetoricalDrawerTriggerProps
> = ({ node, ...props }) => {
  const t = useLocale(i18n);
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));

  const onFinish = useCallback(
    async (data) => {
      const after = await updateComponent(projectId, 'rhetorical', node.id, {
        ...unwrap(node),
        data,
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
    },
    [flowId, node, projectId]
  );
  return (
    <DrawerFormBoxTrigger
      title={t['title']}
      onFinish={onFinish}
      initialValues={node.data}
      {...props}
    >
      <Form.Item
        label={t['form.rhetorical']}
        field="responses[0]"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <Text placeholder={t['form.rhetorical.placeholder']} />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export const UpdateRhetoricalDrawerTrigger: React.FC<
  Omit<RhetoricalDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <RhetoricalDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
