import React, { useMemo } from 'react';
import View, { ViewProps } from '../components/View';
import { IoGitBranchOutline } from 'react-icons/io5';
import { NodeProps, NodeRule } from '../types';
import { getNodeLabel } from '../utils/node';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import { CreateBotDrawerTrigger, initialBotValues } from '../bot';
import { useNodeDrop } from '../../dnd';
import { isEmpty, keyBy } from 'lodash';

export const ConfirmIcon = IoGitBranchOutline;

export interface ConfirmViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}
export const ConfirmView: React.FC<ConfirmViewProps> = ({
  node,
  rules,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<ConfirmIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const ConfirmNode: React.FC<NodeProps> = (props) => {
  const rules = useMemo<NodeRule[]>(
    () => [{ key: 'bot', show: isEmpty(props.children) }],
    [props]
  );
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);
  return (
    <MenuBox
      trigger={<ConfirmView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      {ruleMap['bot'].show && (
        <CreateBotDrawerTrigger
          parent={props}
          initialValues={initialBotValues}
        />
      )}
    </MenuBox>
  );
};
