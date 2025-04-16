import * as React from 'react';
import { RiFunctions } from 'react-icons/ri';
import View, { ViewProps } from '../../components/View';
import { NodeProps, NodeRule } from '../../types';
import { getNodeLabel } from '../../utils/node';
import { useNodeDrop } from '../../../dnd';
import { keyBy } from 'lodash';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import { CreateGptFunctionDrawerTrigger } from '../gpt-function';

export const GptFunctionsIcon = RiFunctions;

export interface GptFunctionsViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}
export const GptFunctionsView: React.FC<GptFunctionsViewProps> = ({
  rules,
  node,
  ...props
}) => {
  const label = React.useMemo(() => getNodeLabel(node), [node]);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<GptFunctionsIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const GptFunctionsNode: React.FC<NodeProps> = (props) => {
  const rules = React.useMemo<NodeRule[]>(
    () => [{ key: 'function-gpt', show: true }],
    []
  );
  const ruleMap = React.useMemo(() => keyBy(rules, 'key'), [rules]);
  return (
    <MenuBox
      trigger={<GptFunctionsView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      {ruleMap['function-gpt'].show && (
        <CreateGptFunctionDrawerTrigger parent={props} />
      )}
    </MenuBox>
  );
};
