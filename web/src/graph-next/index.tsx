import React, { memo } from 'react';
import GraphCore from '@/core-next';
import { GraphTreeValue, GraphCoreProps } from '@/core-next/types';
import { GraphNode } from './type';

export function sampleSelect(value: GraphNode[], node: GraphTreeValue) {
  return value.map((o) => ({ ...o, selected: o.id === node.id }));
}
export function unSelect(value: GraphNode[]) {
  return value.map((v) => ({ ...v, selected: false }));
}
const GraphNext = (props: GraphCoreProps, ref) => {
  return <GraphCore {...props} ref={ref} />;
};

export default memo(React.forwardRef(GraphNext));
