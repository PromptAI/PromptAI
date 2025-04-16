import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { IconEdit, IconSend, IconUserAdd } from '@arco-design/web-react/icon';
import { isEmpty } from 'lodash';
import React, { useMemo } from 'react';
import { creaeteDefaultBot, createDefaultUser } from '../helper';
import { useGraphNodeDrop } from '../hooks';
import { RelationNodeDefinedProps } from '../types';
import { normalGraphNode, useBuildMenus } from '../util';
import FlowView from './view.';

const Flow = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { t, RU } = useBuildMenus();
  const menus = useMemo<PopupMenu[]>(() => {
    const { id } = node;
    const {
      projectId,
      rootComponentId: flowId,
      onChangeSelection,
      onChangeEditSelection,
      refresh,
    } = defaultProps;
    const handleAddUser = async () => {
      const [user, children] = await createDefaultUser(projectId, id, [id]);
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
    const handleAddResponse = async () => {
      const [bot, children] = await creaeteDefaultBot(projectId, id, [id]);
      onChangeSelection(bot);
      onChangeEditSelection(bot);
      refresh();
      RU.push({
        type: 'add_node',
        changed: bot,
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
        key: 'edit',
        title: t['flow.node.edit'],
        icon: <IconEdit />,
        onClick: () => onChangeEditSelection(node),
      },
      {
        key: 'user',
        title: t['flow.node.user'],
        icon: <IconUserAdd />,
        onClick: handleAddUser,
        hidden:
          !isEmpty(node.children) &&
          node.children.some((c) => c.type === 'bot'),
      },
      {
        key: 'bot',
        title: t['flow.node.bot'],
        icon: <IconSend />,
        onClick: handleAddResponse,
        hidden: !isEmpty(node.children),
      },
    ];
  }, [RU, defaultProps, node, t]);

  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  return (
    <Wrapper
      menus={menus}
      selected={node.selected}
      validatorError={{ ...props.validatorError, color: '#FB8C00' }}
    >
      <div {...dropProps}>
        <FlowView {...props} />
      </div>
    </Wrapper>
  );
};

export default Flow;
export { FlowView };
