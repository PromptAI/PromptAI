import { RelationNodeDefinedProps } from '../types';
import { useBuildMenus } from '../util';
import React, { useMemo } from 'react';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { useGraphNodeDrop } from '../hooks';
import AbortView from '../gpt-abort/view';

const NodeGPTAbort = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;
  const { buildMenus } = useBuildMenus();
  const menus = useMemo<PopupMenu[]>(() => {
    return buildMenus({ props }, []);
  }, []);

  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);

  return (
    <Wrapper
      menus={menus}
      selected={node.selected}
      validatorError={node.validatorError}
    >
      <div {...dropProps}>
        <AbortView {...props} />
      </div>
    </Wrapper>
  );
};

export default NodeGPTAbort;
export { AbortView };
