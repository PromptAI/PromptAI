import React, { useMemo } from 'react';
import View, { ViewProps } from '../../components/View';
import { NodeProps, NodeRule } from '../../types';
import { findGotoFilterTargets, getNodeLabel } from '../../utils/node';
import MenuBox from '@/pages/projects/project/components/MenuBox';
import { CreateBotDrawerTrigger, initialBotValues } from '../../bot';
import { useNodeDrop } from '../../../dnd';
import { isEmpty, keyBy } from 'lodash';
import { BsBookmarkX } from 'react-icons/bs';
import { useGraphStore } from '../../../store/graph';
import { CreateGotoDrawerTrigger } from '../../goto';

export const GptAbortIcon = BsBookmarkX;

export interface GptAbortViewProps
  extends Omit<ViewProps, 'icon' | 'id' | 'label'> {
  rules: NodeRule[];
  node: NodeProps;
}
export const GptAbortView: React.FC<GptAbortViewProps> = ({
  node,
  rules,
  ...props
}) => {
  const label = useMemo(() => getNodeLabel(node), [node]);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { draggable, ...dropProps } = useNodeDrop(node, rules);
  return (
    <View
      icon={<GptAbortIcon />}
      id={node.id}
      label={label}
      validatorError={node.validatorError}
      {...props}
      {...dropProps}
    />
  );
};

export const GptAbortNode: React.FC<NodeProps> = (props) => {
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
      trigger={<GptAbortView rules={rules} node={props} />}
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
