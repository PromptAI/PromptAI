import React, { useCallback, useMemo, useRef, useState } from 'react';
import View, { ViewProps } from '../components/View';
import { NodeProps } from '../types';
import { IconCodeBlock, IconEdit } from '@arco-design/web-react/icon';
import { getNodeLabel, parseResponseOfCreate, unwrap } from '../utils/node';
import useLocale from '@/utils/useLocale';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import DeleteNodeTrigger from '../components/DeleteNodeTrigger';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import i18n from './i18n';
import { useGraphStore } from '../../store/graph';
import FavoriteNodeTrigger from '../components/FavoriteNodeTrigger';
import { createComponent, updateComponent } from '@/api/components';
import ru, { RU_TYPE } from '../../features/ru';
import { Alert, Button, Form, FormInstance } from '@arco-design/web-react';

import {
  SlotsProvider,
  Selector,
} from '@/pages/projects/project/components/multivariable';
import common from '../i18n';
import Visible from '@/pages/projects/project/components/Visible';
import { isEmpty } from 'lodash';
import CodeField from '../components/CodeField';
import TextArea from '@/pages/projects/project/components/TextArea';
import { useNodeDrop } from '../../dnd';

export const FieldIcon = IconCodeBlock;

const emptyRules = [];
export interface FieldViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const FieldView: React.FC<FieldViewProps> = ({ node, ...props }) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  const dropProps = useNodeDrop(node, emptyRules);
  return (
    <View
      icon={<FieldIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const FieldNode: React.FC<NodeProps> = (props) => {
  return (
    <MenuBox
      trigger={<FieldView node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateFieldDrawerTrigger node={props} />
      <DeleteNodeTrigger node={props} />
      <MenuBoxDivider />
       {/*<FavoriteNodeTrigger node={props} />*/}
    </MenuBox>
  );
};

const validateDefaultValue = `# \${slot_name} will be automatically replaced by the system
def validate_\${slot_name}(
    self,
    slot_value: Any,
    dispatcher: CollectingDispatcher,
    tracker: Tracker,
    domain: DomainDict,) -> Dict[Text, Any]:

    """Validate slot_name value."""
    
    return {"\${slot_name}": slot_value}`;

const extractDefaultValue = `# don't edit this function\`s name
async def extract_\${slot_name}(
    self,
    dispatcher: CollectingDispatcher,
    tracker: Tracker,
    domain: Dict,) -> Dict[Text, Any]:

    text_of_last_user_message = tracker.latest_message.get("text")
    sit_outside = "outdoor" in text_of_last_user_message
    
    return {"\${slot_name}": sit_outside}`;
const canbeChangeSlotId = (node) => {
  if (node.children && node.children[0] && node.children[0].children?.[0]) {
    const definedCollectNode = node.children[0].children[0];
    if (
      definedCollectNode.data.mappingsEnable &&
      !isEmpty(definedCollectNode.data.mappings)
    ) {
      return false;
    }
  }
  return true;
};
export interface FieldDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
}
export const FieldDrawerTrigger: React.FC<FieldDrawerTriggerProps> = ({
  mode,
  node,
  ...props
}) => {
  const formRef = useRef<FormInstance>();
  const t = useLocale(i18n);
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));
  const [alertMessage, setAlertMessage] = useState<string>(null);
  const onCreateSlot = useCallback(
    ({ slotId }) => {
      if (node.data.slotId && !canbeChangeSlotId(node)) {
        setAlertMessage(t['form.slotId.warning']);
        formRef.current.setFieldValue('slotId', node.data.slotId);
        return;
      }
      formRef.current.setFieldValue('slotId', slotId);
    },
    [node, t]
  );

  const onFinish = useCallback(
    async (data) => {
      if (mode === 'update') {
        const after = await updateComponent(projectId, 'field', node.id, {
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
      }
      if (mode === 'create') {
        const nodes = await createComponent(projectId, 'field', {
          data,
          parentId: node.id,
        });
        const [changed, children] = parseResponseOfCreate(nodes, 'field');
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
    [flowId, node, projectId, mode]
  );
  return (
    <DrawerFormBoxTrigger
      ref={formRef}
      title={t['title']}
      initialValues={node.data}
      onFinish={onFinish}
      {...props}
    >
      {alertMessage && <Alert type="error" content={alertMessage} />}
      <SlotsProvider>
        <Form.Item
          label={t['form.slotId']}
          field="slotId"
          rules={[{ required: true, message: t['rule.required'] }]}
        >
          <Selector onCreate={onCreateSlot} />
        </Form.Item>
      </SlotsProvider>
      <Form.Item label={t['form.validatedCode']} field="validatedCode">
        <CodeField
          defaultValue={validateDefaultValue}
          title={t['form.validatedCode']}
        />
      </Form.Item>
      <Form.Item label={t['form.extractCode']} field="extractCode">
        <CodeField
          defaultValue={extractDefaultValue}
          title={t['form.extractCode']}
        />
      </Form.Item>
      <Form.Item label={t['form.description']} field="description">
        <TextArea placeholder={t['form.description']} />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateFieldDrawerTriggerProps
  extends Omit<
    FieldDrawerTriggerProps,
    'mode' | 'trigger' | 'node' | 'refresh'
  > {
  parent: Partial<NodeProps>;
}
export const CreateFieldDrawerTrigger: React.FC<
  CreateFieldDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <FieldDrawerTrigger
        mode="create"
        node={parent}
        trigger={<Button icon={<FieldIcon />}>{t['node.add']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};

export const UpdateFieldDrawerTrigger: React.FC<
  Omit<FieldDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <FieldDrawerTrigger
        mode="update"
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
