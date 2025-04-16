import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import React, { useMemo } from 'react';
import { useGraphNodeDrop } from '../hooks';
import { RelationNodeDefinedProps } from '../types';
import { useBuildMenus } from '../util';
import FormView from './view';

const NodeForm = (props: RelationNodeDefinedProps) => {
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
        <FormView {...props} />
      </div>
    </Wrapper>
  );
};

export default NodeForm;
export { FormView };
