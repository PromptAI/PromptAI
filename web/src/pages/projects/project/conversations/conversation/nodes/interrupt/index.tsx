import React, { useMemo } from 'react';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { RelationNodeDefinedProps } from '../types';
import { IconUserAdd } from '@arco-design/web-react/icon';
import { useGraphNodeDrop } from '../hooks';
import InterruptView from './view';
import { normalGraphNode, useBuildMenus } from '../util';
import { createDefaultUser } from '../helper';

const Interrupt = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { RU, t } = useBuildMenus();
  const menus = useMemo<PopupMenu[]>(() => {
    const {
      projectId,
      rootComponentId: flowId,
      onChangeSelection,
      onChangeEditSelection,
      refresh,
    } = defaultProps;
    const handleAddIntent = async () => {
      const { id } = node;
      const [user, children] = await createDefaultUser(projectId, id, [flowId]);
      onChangeSelection(user);
      onChangeEditSelection(user);
      refresh();
      RU.push({
        type: 'add_node',
        changed: user,
        dependencies: {
          projectId,
          parent: normalGraphNode(node),
          children,
          flowId,
        },
      });
    };
    return [
      {
        key: 'user',
        title: t['flow.node.user'],
        icon: <IconUserAdd />,
        onClick: handleAddIntent,
      },
    ];
  }, [RU, defaultProps, node, t]);
  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  return (
    <Wrapper
      menus={menus}
      selected={node.selected}
      validatorError={props.validatorError}
    >
      <div {...dropProps}>
        <InterruptView />
      </div>
    </Wrapper>
  );
};

export default Interrupt;
export { InterruptView };
