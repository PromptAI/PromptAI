import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { IconEdit, IconUserAdd } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import { nanoid } from 'nanoid';
import React, { useMemo } from 'react';
import { createDefaultRhetoricalUser } from '../helper';
import { useGraphNodeDrop } from '../hooks';
import { RelationNodeDefinedProps } from '../types';
import { normalGraphNode, useBuildMenus } from '../util';
import RhetoricalView from './view';

const Rhetorical = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { t, RU } = useBuildMenus();
  const menus = useMemo<PopupMenu[]>(() => {
    const {
      projectId,
      rootComponentId: flowId,
      onChangeSelection,
      onChangeEditSelection,
      refresh,
    } = defaultProps;
    const handleAddIntent = async () => {
      const { id, parent } = node;
      const { slotId, slotName } = parent.data;
      const [user, children] = await createDefaultRhetoricalUser(
        projectId,
        id,
        {
          name: '',
          examples: [],
          mappingsEnable: true,
          mappings: [
            {
              id: nanoid(),
              slotId,
              slotName,
              type: null,
              enable: false,
            },
          ],
        },
        [flowId],
        id
      );
      onChangeSelection(user);
      onChangeEditSelection(user);
      refresh();
      RU.push({
        type: 'add_node',
        changed: user,
        dependencies: {
          parent: normalGraphNode(node),
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
        key: 'user',
        title: t['flow.node.user'],
        icon: <IconUserAdd />,
        onClick: handleAddIntent,
        hidden: isEmpty(node.parent?.data.slotId),
        divider: true,
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
        <RhetoricalView {...props} />
      </div>
    </Wrapper>
  );
};

export default Rhetorical;
export { RhetoricalView };
