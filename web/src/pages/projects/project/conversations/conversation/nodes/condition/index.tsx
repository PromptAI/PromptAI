import { isEmpty } from 'lodash';
import React, { useMemo } from 'react';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { IconSend, IconUserAdd } from '@arco-design/web-react/icon';
import { RelationNodeDefinedProps } from '../types';
import { useGraphNodeDrop } from '../hooks';
import { normalGraphNode, useBuildMenus } from '../util';
import ConditionView from './view';
import { creaeteDefaultBot, createDefaultUser } from '../helper';

const Condition = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { t, buildMenus, RU } = useBuildMenus();
  const menus = useMemo<PopupMenu[]>(() => {
    const { defaultProps, ...node } = props;
    const {
      projectId,
      rootComponentId: flowId,
      onChangeSelection,
      onChangeEditSelection,
      refresh,
    } = defaultProps;
    const { id } = node;
    const handleAddResponse = async () => {
      const [bot, children] = await creaeteDefaultBot(projectId, id, [flowId]);
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
    return buildMenus({ props }, [
      {
        key: 'bot',
        title: t['flow.node.bot'],
        icon: <IconSend />,
        onClick: handleAddResponse,
        hidden: !isEmpty(node.children),
        divider: true,
      },
      {
        key: 'user',
        title: t['flow.node.user'],
        icon: <IconUserAdd />,
        onClick: handleAddIntent,
        hidden: !isEmpty(node.children),
      },
    ]);
  }, [RU, buildMenus, props, t]);
  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  return (
    <Wrapper
      menus={menus}
      selected={node.selected}
      validatorError={props.validatorError}
    >
      <div {...dropProps}>
        <ConditionView {...props} />
      </div>
    </Wrapper>
  );
};

export default Condition;
export { ConditionView };
