import React from 'react';
import { linkHorizontal } from 'd3-shape';
import { GraphEdge } from './types';
import { useCreation } from 'ahooks';

const SmoothEdge = ({ edge }: { edge: GraphEdge }) => {
  const d = useCreation(() => linkHorizontal()(edge), [edge]);
  return (
    <g>
      <path d={d} className="mind-graph-default-link" />
    </g>
  );
};
export default SmoothEdge;
