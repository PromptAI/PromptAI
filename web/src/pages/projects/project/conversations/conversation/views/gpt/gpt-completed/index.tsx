import React, { useMemo } from 'react';
import View, { ViewProps } from '../../components/View';
import { NodeProps, NodeRule } from '../../types';
import { findGotoFilterTargets, getNodeLabel } from '../../utils/node';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import { CreateBotDrawerTrigger, initialBotValues } from '../../bot';
import { useNodeDrop } from '../../../dnd';
import { isEmpty, keyBy } from 'lodash';
import { BsBookmarkCheck } from 'react-icons/bs';
import { CreateGotoDrawerTrigger } from '../../goto';
import { useGraphStore } from '../../../store/graph';

export const GptCompletedIcon = BsBookmarkCheck;

export interface GptCompletedViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}
export const GptCompletedView: React.FC<GptCompletedViewProps> = ({
  node,
  rules,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<GptCompletedIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const GptCompletedNode: React.FC<NodeProps> = (props) => {
  const nodes = useGraphStore((s) =>
    s.nodes.filter((s) => !['conversation'].includes(s.type))
  );
  const rules = useMemo<NodeRule[]>(() => {
    const emptyChildren = isEmpty(props.children);
    const filters = findGotoFilterTargets(props);
    const targets = nodes.filter((n) => !filters.some((f) => f.id === n.id));
    return [
      { key: 'bot', show: emptyChildren },
      {
        key: 'goto',
        show: targets.length > 0 && emptyChildren,
      },
    ];
  }, [props, nodes]);
  const ruleMap = useMemo(() => keyBy(rules, 'key'), [rules]);
  return (
    <MenuBox
      trigger={<GptCompletedView rules={rules} node={props} />}
      validatorError={props.validatorError}
    >
      {ruleMap['bot'].show && (
        <CreateBotDrawerTrigger
          parent={props}
          initialValues={initialBotValues}
        />
      )}
      {ruleMap['goto'].show && <CreateGotoDrawerTrigger parent={props} />}
    </MenuBox>
  );
};
