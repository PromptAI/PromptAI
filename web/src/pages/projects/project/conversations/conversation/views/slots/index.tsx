import React, { useCallback, useMemo } from 'react';
import View, { ViewProps } from '../components/View';
import { VscSymbolVariable } from 'react-icons/vsc';
import { NodeProps, NodeRule } from '../types';
import { getNodeLabel, unwrap } from '../utils/node';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import {
  DrawerFormBoxTrigger,
  DrawerFormBoxTriggerProps,
} from '../../../../components/DrawerBox';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import { useGraphStore } from '../../store/graph';
import { Button, Form } from '@arco-design/web-react';
import Example from '../user/components/Example';
import ExamplesFormItem from '../user/components/ExamplesFormItem';
import common from '../i18n';
import Visible from '@/pages/projects/project/components/Visible';
import { IconEdit } from '@arco-design/web-react/icon';
import useDocumentLinks from '@/hooks/useDocumentLinks';
import CodeField from '../components/CodeField';
import { unwrapFormValues, wrapFormValues } from '../user/utils';
import { updateComponent } from '@/api/components';
import ru, { RU_TYPE } from '../../features/ru';
import { isBlank } from '@/utils/is';
import { useNodeDrop } from '../../dnd';
import { keyBy } from 'lodash';
import { CreateFieldDrawerTrigger } from '../field';

export const SlotsIcon = VscSymbolVariable;

export interface SlotsViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}

export const SlotsView: React.FC<SlotsViewProps> = ({
  node,
  rules,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<SlotsIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const SlotsNode: React.FC<NodeProps> = (props) => {
  const rules = useMemo<NodeRule[]>(
    () => [
      {
        key: 'field',
        show: true,
      },
    ],
    []
  );
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);
  return (
    <MenuBox
      trigger={<SlotsView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      <UpdateSlotsDrawerTrigger node={props} />
      {ruleMap['field'].show && <CreateFieldDrawerTrigger parent={props} />}
    </MenuBox>
  );
};

const defaultValue = `# don't edit this function\`s name
async def required_slots(
    self,
    domain_slots: List[Text],
    dispatcher: CollectingDispatcher,
    tracker: Tracker,
    domain: DomainDict,) -> List[Text]:

    # additional_slots = ["append_slot_name"]
    # if tracker.slots.get("append_slot_name") is True:
      # If the user wants to sit outside, ask
      # if they want to sit in the shade or in the sun.
      # additional_slots.append("append_other_slot_name")
      # return required_slots
    
    return domain_slots`;
export interface SlotsDrawerTriggerProps extends DrawerFormBoxTriggerProps {
  node: Partial<NodeProps>;
}
export const SlotsDrawerTrigger: React.FC<SlotsDrawerTriggerProps> = ({
  node,
  ...props
}) => {
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));
  const t = useLocale(i18n);
  const docs = useDocumentLinks();
  const initialValues = wrapFormValues(node.data);
  const onFinish = useCallback(
    async (data) => {
      data = unwrapFormValues(data);
      data.examples = data.examples?.filter((e) => !isBlank(e.text));
      const after = await updateComponent(projectId, 'slots', node.id, {
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
      initialValues={initialValues}
      onFinish={onFinish}
      {...props}
    >
      <Form.Item label={t['form.examples.0']} field="mainExample">
        <Example markEnable entities={[]} placeholder={t['form.examples.0']} />
      </Form.Item>
      <Form.Item label={t['form.examples']} field="examples">
        <Form.List field="examples">
          {(fields, operation) => (
            <ExamplesFormItem
              fields={fields}
              operation={operation}
              markEnable
              entities={[]}
              placeholder={t['form.examples.placeholder']}
            />
          )}
        </Form.List>
      </Form.Item>
      <Form.Item
        label={t['form.required.slots.code']}
        field="requiredSlotsCode"
      >
        <CodeField
          defaultValue={defaultValue}
          title={t['form.required.slots.code']}
          titleLink={docs.dynamicSlot}
        />
      </Form.Item>
    </DrawerFormBoxTrigger>
  );
};

export const UpdateSlotsDrawerTrigger: React.FC<
  Omit<SlotsDrawerTriggerProps, 'trigger' | 'refresh'>
> = (props) => {
  const c = useLocale(common);
  const refresh = useGraphStore((s) => s.refresh);
  return (
    <Visible>
      <SlotsDrawerTrigger
        trigger={<Button icon={<IconEdit />}>{c['node.edit']}</Button>}
        refresh={refresh}
        {...props}
      />
    </Visible>
  );
};
