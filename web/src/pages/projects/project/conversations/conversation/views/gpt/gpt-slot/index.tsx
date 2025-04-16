import * as React from 'react';

import View, { ViewProps } from '../../components/View';
import { NodeProps } from '../../types';
import { IconCodeBlock, IconEdit } from '@arco-design/web-react/icon';
import {
  createNodeFetch,
  getNodeLabel,
  updateNodeFetch,
} from '../../utils/node';
import useLocale from '@/utils/useLocale';
import MenuBox, {
  MenuBoxDivider,
} from '@/pages/projects/project/components/MenuBox';
import DeleteNodeTrigger from '../../components/DeleteNodeTrigger';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../../components/DrawerBox';
import i18n from './i18n';
import { useGraphStore } from '../../../store/graph';
import FavoriteNodeTrigger from '../../components/FavoriteNodeTrigger';
import { Button, Collapse, Form, FormInstance } from '@arco-design/web-react';

import {
  SlotsProvider,
  Selector,
  useSlotsContext,
} from '@/pages/projects/project/components/multivariable';
import common from '../../i18n';
import Visible from '@/pages/projects/project/components/Visible';
import { useNodeDrop } from '../../../dnd';
import TextArea from '@/pages/projects/project/components/TextArea';
import SlotTypeFormItem from '@/pages/projects/project/components/SlotTypeFormItem';
import { Slot } from '@/graph-next/type';
import { RefTextAreaType } from '@arco-design/web-react/es/Input';
import { omit, pick } from 'lodash';
import { updateCompSlot } from '@/api/components';
import { InvalidParamsError } from '@/errors';

export const GptSlotIcon = IconCodeBlock;

const emptyRules = [];
export interface GptSlotViewProps
  extends Omit<ViewProps, 'id' | 'label' | 'icon'> {
  node: NodeProps;
}
export const GptSlotView: React.FC<GptSlotViewProps> = ({ node, ...props }) => {
  const label = React.useMemo(() => getNodeLabel(node), [node]);
  const dropProps = useNodeDrop(node, emptyRules);
  return (
    <View
      icon={<GptSlotIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const GptSlotNode: React.FC<NodeProps> = (props) => {
  return (
    <MenuBox
      trigger={<GptSlotView node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateGptSlotDrawerTrigger node={props} />
      <DeleteNodeTrigger node={props} />
      <MenuBoxDivider />
       {/*<FavoriteNodeTrigger node={props} />*/}
    </MenuBox>
  );
};

export interface GptSlotDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  mode: 'create' | 'update';
  node: Partial<NodeProps>;
}
const SLOT_TYPE_FIELDS = [
  'type',
  'enumEnable',
  'enum',
  'defaultValueEnable',
  'defaultValueType',
  'defaultValue',
];

export const GptSlotDrawerTrigger: React.FC<GptSlotDrawerTriggerProps> = ({
  mode,
  node,
  ...props
}) => {
  const formRef = React.useRef<FormInstance>();
  const t = useLocale(i18n);
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));

  const onCreateSlot = React.useCallback((_, slot: Slot) => {
    formRef.current.setFieldsValue({
      slotId: slot.id,
      type: slot.slotType,
      enumEnable: slot.enumEnable,
      enum: slot.enum,
      defaultValueEnable: slot.defaultValueEnable,
      defaultValue: slot.defaultValue,
      defaultValueType: slot.defaultValueType || 'set',
    });
  }, []);

  const { map, refresh } = useSlotsContext();

  const rhetoricalRef = React.useRef<RefTextAreaType>();

  const onValuesChange = React.useCallback(
    (field) => {
      if (field.slotId !== undefined) {
        const slot = map[field.slotId];
        if (slot) {
          formRef.current.setFieldsValue({
            type: slot.slotType,
            enumEnable: slot.enumEnable,
            enum: slot.enum,
            defaultValueEnable: slot.defaultValueEnable,
            defaultValue: slot.defaultValue,
            defaultValueType: slot.defaultValueType || 'set',
          });
          setTimeout(() => rhetoricalRef.current?.focus());
        }
      }
    },
    [map]
  );

  const onFinish = React.useCallback(
    async (data) => {
      const nodeData: any = omit(data, SLOT_TYPE_FIELDS);
      const { type: slotType, ...slotRest }: any = pick(data, SLOT_TYPE_FIELDS);
      const remoteSlot = map[nodeData.slotId];
      if (!remoteSlot) throw new InvalidParamsError('invalid slot id');
      await updateCompSlot({
        ...remoteSlot,
        ...slotRest,
        slotType,
        projectId,
        id: nodeData.slotId,
      });
      if (mode === 'update') {
        await updateNodeFetch(node, nodeData, 'slot-gpt', {
          projectId,
          flowId,
        });
      }
      if (mode === 'create') {
        await createNodeFetch(node, nodeData, 'slot-gpt', {
          flowId,
          projectId,
        });
      }
    },
    [flowId, map, mode, node, projectId]
  );
  const [initialValues, setInitialValues] = React.useState(node.data);

  React.useEffect(() => {
    if (props.visible) {
      refresh();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.visible]);
  React.useEffect(() => {
    if (props.visible) {
      setTimeout(() => {
        const slot = map[node.data?.slotId];
        if (slot) {
          setInitialValues({
            ...node.data,
            type: slot.slotType,
            enumEnable: slot.enumEnable,
            enum: slot.enum,
            defaultValueEnable: slot.defaultValueEnable,
            defaultValue: slot.defaultValue,
            defaultValueType: slot.defaultValueType || 'set',
          });
          formRef.current.setFieldsValue({
            type: slot.slotType,
            enumEnable: slot.enumEnable,
            enum: slot.enum,
            defaultValueEnable: slot.defaultValueEnable,
            defaultValue: slot.defaultValue,
            defaultValueType: slot.defaultValueType || 'set',
          });
          rhetoricalRef.current?.focus();
        }
      });
    }
  }, [map, node.data, props.visible]);
  const hiddenKeys = React.useMemo(() => {
    if (node.type === 'slot-gpt') {
      return (
        node.parent?.children
          ?.filter((c) => c.id !== node.id)
          .map((c) => c.data.slotId) || []
      );
    }
    if (node.type === 'slots-gpt') {
      return node.children?.map((c) => c.data.slotId) || [];
    }
    return [];
  }, [node]);

  return (
    <DrawerFormBoxTrigger
      ref={formRef}
      title={t['title']}
      initialValues={initialValues}
      onFinish={onFinish}
      onValuesChange={onValuesChange}
      {...props}
    >
      <Form.Item
        label={t['form.slotId']}
        field="slotId"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <Selector
          onCreate={onCreateSlot}
          hiddenCreator
          hiddenKeys={hiddenKeys}
        />
      </Form.Item>
      <Form.Item
        label={t['form.slotQuestion']}
        field="rhetorical"
        rules={[{ required: true, message: t['rule.required'] }]}
      >
        <TextArea
          placeholder={t['form.slotQuestion.placeholder']}
          ref={rhetoricalRef}
        />
      </Form.Item>
      <Form.Item label={t['form.description']} field="description">
        <TextArea placeholder={t['form.description.placeholder']} />
      </Form.Item>
      <Form.Item>
        <Collapse defaultActiveKey="slotType">
          <Collapse.Item
            header={t['form.slotType.header']}
            name="slotType"
            contentStyle={{ padding: '1rem' }}
          >
            <SlotTypeFormItem />
          </Collapse.Item>
        </Collapse>
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export interface CreateGptSlotDrawerTriggerProps
  extends Omit<
    GptSlotDrawerTriggerProps,
    'mode' | 'trigger' | 'node' | 'refresh'
  > {
  parent: Partial<NodeProps>;
}
export const CreateGptSlotDrawerTrigger: React.FC<
  CreateGptSlotDrawerTriggerProps
> = ({ parent, ...props }) => {
  const t = useLocale(i18n);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <SlotsProvider needMap>
      <Visible>
        <GptSlotDrawerTrigger
          mode="create"
          node={parent}
          trigger={<Button icon={<GptSlotIcon />}>{t['node.add']}</Button>}
          refresh={refresh}
          {...props}
        />
      </Visible>
    </SlotsProvider>
  );
};

export const UpdateGptSlotDrawerTrigger: React.FC<
  Omit<GptSlotDrawerTriggerProps, 'mode' | 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <SlotsProvider needMap>
      <Visible>
        <GptSlotDrawerTrigger
          mode="update"
          trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
          refresh={refresh}
          {...props}
        />
      </Visible>
    </SlotsProvider>
  );
};
