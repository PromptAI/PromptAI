import * as React from 'react';
import { GraphTreeTemplateProps, NodeDefinedProps } from './types';
import cn from '@/utils/cn';

const DefaultNode = (props: NodeDefinedProps) => <div>{props.id}</div>;

const Template = ({
  node,
  Component = DefaultNode,
  positionHub,
  onSelect,
  onDoubleSelect,
  defaultProps,
  children,
}: GraphTreeTemplateProps) => {
  const ref = React.useRef<HTMLDivElement>();
  // useCachePoint(node, [ref, innerRef], anchorConfig, expand, name);
  const hanldeClick = React.useCallback(
    (evt) => {
      evt.stopPropagation();
      onSelect?.(node);
    },
    [node, onSelect]
  );
  const handleDoubleClick = React.useCallback(
    (evt) => {
      evt.stopPropagation();
      onDoubleSelect?.(node);
    },
    [node, onDoubleSelect]
  );

  React.useEffect(() => {
    const { left, top, width, height } = ref.current.getBoundingClientRect();
    setTimeout(() => {
      const svg = document
        .getElementById(positionHub.name)
        .getBoundingClientRect();

      positionHub.set(
        node.id,
        new DOMRect(left - svg.left, top - svg.top, width, height)
      );
    });
  }, [node, positionHub]);
  return (
    <div className={cn('mind-graph-node', defaultProps?.nodeClassClassName)}>
      <div
        className={cn('mind-graph-node-inner', defaultProps?.innerClassName)}
        data-node-id={node.id}
        ref={ref}
        onClick={hanldeClick}
        onDoubleClick={handleDoubleClick}
      >
        <Component {...node} />
      </div>
      {children && (
        <div
          className={cn(
            'mind-graph-node-children',
            defaultProps?.childrenClassName
          )}
        >
          {children}
        </div>
      )}
    </div>
  );
};

export default Template;
