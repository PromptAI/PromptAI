import { isEmpty } from 'lodash';
import React, { useMemo } from 'react';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { IconSend } from '@arco-design/web-react/icon';
import { RelationNodeDefinedProps } from '../types';
import { useGraphNodeDrop } from '../hooks';
import ConfirmView from './view';
import { normalGraphNode, useBuildMenus } from '../util';
import { creaeteDefaultBot } from '../helper';

const Confirm = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { t, RU } = useBuildMenus();
  const menus = useMemo<PopupMenu[]>(() => {
    const { defaultProps, ...node } = props;
    if (isEmpty(node.children)) {
      const {
        projectId,
        rootComponentId: flowId,
        onChangeSelection,
        onChangeEditSelection,
        refresh,
      } = defaultProps;
      const { id } = node;
      const handleAddResponse = async () => {
        const [bot, children] = await creaeteDefaultBot(projectId, id, [
          flowId,
        ]);
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
          key: 'bot',
          title: t['flow.node.bot'],
          icon: <IconSend />,
          onClick: handleAddResponse,
        },
      ];
    }
    return [];
  }, [RU, props, t]);
  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  return (
    <Wrapper
      menus={menus}
      selected={node.selected}
      validatorError={props.validatorError}
    >
      <div {...dropProps}>
        <ConfirmView />
      </div>
    </Wrapper>
  );
};

export default Confirm;
export { ConfirmView };
