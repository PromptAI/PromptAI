import React, { useMemo } from 'react';
import View, { ViewProps } from '../components/View';
import { IoGitBranchOutline } from 'react-icons/io5';
import { NodeProps, NodeRule } from '../types';
import { getNodeLabel } from '../utils/node';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import { CreateUserDrawerTrigger } from '../user';
import { useNodeDrop } from '../../dnd';
import { keyBy } from 'lodash';

export const InterruptIcon = IoGitBranchOutline;

export interface InterruptViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}
export const InterruptView: React.FC<InterruptViewProps> = ({
  node,
  rules,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<InterruptIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const InterruptNode: React.FC<NodeProps> = (props) => {
  const rules = useMemo<NodeRule[]>(
    () => [
      {
        key: 'user',
        show: true,
      },
    ],
    []
  );
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);
  return (
    <MenuBox
      trigger={<InterruptView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      {ruleMap['user'].show && <CreateUserDrawerTrigger parent={props} />}
    </MenuBox>
  );
};
