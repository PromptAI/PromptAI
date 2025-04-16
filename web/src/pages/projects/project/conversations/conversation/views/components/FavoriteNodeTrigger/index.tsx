import React, { useCallback } from 'react';
import i18n from './i18n';
import { NodeProps } from '../../types';
import useLocale from '@/utils/useLocale';
import { expendTree } from '@/core-next/utils';
import { doFavorite } from '@/api/favorites';
import { useGraphStore } from '../../../store/graph';
import { Button, Message } from '@arco-design/web-react';
import MenuBox from '../../../../../components/MenuBox';
import { IconRight, IconStar, IconStarFill } from '@arco-design/web-react/icon';
import { useFavorites } from '../../../favorites/context';

interface FavoriteNodeTriggerProps {
  node: NodeProps;
}
const FavoriteNodeTrigger: React.FC<FavoriteNodeTriggerProps> = ({ node }) => {
  const t = useLocale(i18n);
  const { projectId, flowId } = useGraphStore(({ projectId, flowId }) => ({
    projectId,
    flowId,
  }));
  const { setVisible, refreshFavorites } = useFavorites();
  const onClick = useCallback(
    async (withChildren = false) => {
      const componentIds = withChildren
        ? expendTree(node).map((n) => n.id)
        : [node.id];
      await doFavorite({
        projectId,
        rootComponentId: flowId,
        type: 'conversation',
        componentIds,
      });
      // refresh favorite panel
      setVisible(true);
      refreshFavorites();
      Message.success(t['favorite.success']);
    },
    [flowId, node, projectId, refreshFavorites, setVisible, t]
  );
  return (
    <MenuBox
      action="hover"
      trigger={
        <Button icon={<IconStarFill />} className="!flex items-center">
          <span className="flex items-center gap-2 justify-between flex-1">
            {t['favorite']}
            <IconRight />
          </span>
        </Button>
      }
      triggerProps={{ position: 'rt' }}
    >
      <Button icon={<IconStar />} onClick={() => onClick()}>
        {t['favorite.current']}
      </Button>
      <Button icon={<IconStar />} onClick={() => onClick(true)}>
        {t['favorite.nodes']}
      </Button>
    </MenuBox>
  );
};

export default FavoriteNodeTrigger;
