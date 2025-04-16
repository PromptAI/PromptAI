import { RelationNodeDefinedProps } from '../types';
import { useBuildMenus } from '../util';
import React, { useMemo } from 'react';
import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import { useGraphNodeDrop } from '../hooks';
import CompleteView from '../gpt-complete/view';

const NodeGPTComplete = (props: RelationNodeDefinedProps) => {
  const { defaultProps, ...node } = props;

  const { buildMenus } = useBuildMenus();

  const menus = useMemo<PopupMenu[]>(() => {
    return buildMenus({ props }, []);
  }, [buildMenus, props]);

  const dropProps = useGraphNodeDrop(menus, node, defaultProps.refresh);
  return (
    <Wrapper
      menus={menus}
      selected={node.selected}
      validatorError={props.validatorError}
    >
      <div {...dropProps}>
        <CompleteView {...props} />
      </div>
    </Wrapper>
  );
};

export default NodeGPTComplete;
export { CompleteView };
