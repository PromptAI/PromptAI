import * as React from 'react';
import { VscSymbolVariable } from 'react-icons/vsc';
import View, { ViewProps } from '../../components/View';
import { NodeProps, NodeRule } from '../../types';
import { getNodeLabel } from '../../utils/node';
import { useNodeDrop } from '../../../dnd';
import { keyBy } from 'lodash';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import { CreateGptSlotDrawerTrigger } from '../gpt-slot';

export const GptSlotsIcon = VscSymbolVariable;

export interface GptSlotsViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}
export const GptSlotsView: React.FC<GptSlotsViewProps> = ({
  node,
  rules,
  ...props
}) => {
  const label = React.useMemo(() => getNodeLabel(node), [node]);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<GptSlotsIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const GptSlotsNode: React.FC<NodeProps> = (props) => {
  const rules = React.useMemo<NodeRule[]>(
    () => [
      {
        key: 'slot-gpt',
        show: true,
      },
    ],
    []
  );
  const ruleMap = React.useMemo(() => keyBy(rules, 'key'), [rules]);
  return (
    <MenuBox
      trigger={<GptSlotsView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      {ruleMap['slot-gpt'].show && (
        <CreateGptSlotDrawerTrigger parent={props} />
      )}
    </MenuBox>
  );
};
