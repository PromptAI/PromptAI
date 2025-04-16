import Wrapper, { PopupMenu } from '@/graph-next/Wrapper';
import React, { useMemo } from 'react';
import { useGraphNodeDrop } from '../hooks';
import { RelationNodeDefinedProps } from '../types';
import { useBuildMenus } from '../util';
import FieldView from './view';

const Field = (props: RelationNodeDefinedProps) => {
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
        <FieldView {...props} />
      </div>
    </Wrapper>
  );
};

export default Field;
export { FieldView };
