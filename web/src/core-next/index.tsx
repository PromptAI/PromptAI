import React, { memo, useEffect, useMemo, useRef } from 'react';
import { GraphCoreProps } from './types';
import './index.css';
import useGraphTree from './useGraphTree';
import Template from './Template';
import useGraphEdge from './useGraphEdge';
import store from './store';
import PositionHub from './PositionHub';

const GraphCore = (
  {
    name,
    value,
    width,
    height,
    nodes,
    canvasClassName,
    onSelect,
    onDoubleSelect,
    onCanvasClick,
    disabledMoveAndZoom,
    children,
  }: GraphCoreProps,
  ref
) => {
  const positionHub = useMemo(() => new PositionHub(name), [name]);

  const { treeValue, treeComponent } = useGraphTree(
    value,
    Template,
    nodes,
    positionHub,
    onSelect,
    onDoubleSelect
  );
  const { edgeComponent } = useGraphEdge(treeValue, positionHub);

  const actionRef = useRef<HTMLDivElement>();
  const controlRef = useRef<HTMLDivElement>();
  useEffect(() => {
    if (!disabledMoveAndZoom) {
      store.getInstance(name).initial(controlRef.current, actionRef.current);
      return () => {
        store.removeInstance(name);
      };
    }
  }, [name, disabledMoveAndZoom]);

  return (
    <div
      className="mind-graph-node-wrapper"
      style={{ width, height }}
      ref={ref}
      onContextMenu={(evt) => evt.preventDefault()}
    >
      <div className="mind-graph" onClick={onCanvasClick}>
        <div className="mind-graph-canvas-over" ref={controlRef}>
          <div
            className={canvasClassName || 'mind-graph-canvas'}
            ref={actionRef}
          >
            {edgeComponent && (
              <svg id={name} className="mind-graph-link">
                {edgeComponent}
              </svg>
            )}
            {treeComponent}
          </div>
        </div>
        <div onClick={(evt) => evt.stopPropagation()}>{children}</div>
      </div>
    </div>
  );
};

export default memo(React.forwardRef(GraphCore));
