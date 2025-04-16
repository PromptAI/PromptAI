import React, { useMemo } from 'react';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { RelationNodeDefinedProps } from '../types';
import { IconEdit, IconPlus } from '@arco-design/web-react/icon';
import { useGraphNodeDrop } from '../hooks';
import SlotsView from './view';
import { normalGraphNode, useBuildMenus } from '../util';
import { createDefaultField } from '../helper';

const Slots = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { t, RU } = useBuildMenus();
  const menus = useMemo<PopupMenu[]>(() => {
    const { defaultProps, ...node } = props;
    const {
      projectId,
      rootComponentId: flowId,
      onChangeSelection,
      onChangeEditSelection,
      refresh,
    } = defaultProps;
    const handleAddSlot = async () => {
      const { id } = node;
      const [field, children] = await createDefaultField(
        projectId,
        id,
        { slotId: null, slotName: '' },
        [flowId]
      );
      onChangeSelection(field);
      onChangeEditSelection(field);
      refresh();
      RU.push({
        type: 'add_node',
        changed: field,
        dependencies: {
          parent: normalGraphNode(field),
          projectId,
          children,
          flowId,
        },
      });
    };
    return [
      {
        key: 'edit',
        title: t['flow.node.edit'],
        icon: <IconEdit />,
        onClick: () => onChangeEditSelection(node),
      },
      {
        key: 'field',
        title: t['flow.node.field'],
        icon: <IconPlus />,
        onClick: handleAddSlot,
      },
    ];
  }, [RU, props, t]);
  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  return (
    <Wrapper
      menus={menus}
      selected={node.selected}
      validatorError={props.validatorError}
    >
      <div {...dropProps}>
        <SlotsView />
      </div>
    </Wrapper>
  );
};

export default Slots;
export { SlotsView };
