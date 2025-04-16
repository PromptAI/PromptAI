import React from 'react';
import PositionHub from './PositionHub';

type Point = [number, number];

export interface GraphValue {
  id: string;
  parentId: string;
  type: string;
  data?: any;
  selected?: boolean;
  first?: boolean;
  no?: number;
  parent?:any
}
export interface GraphTreeValue extends GraphValue {
  parent?: GraphTreeValue;
  children?: GraphTreeValue[];
  subChildren?: GraphTreeValue[];
}
export interface NodeDefinedProps extends GraphTreeValue {
  defaultProps?: Record<string, any>;
}
export interface RateAndOffset {
  rate: number;
  offset: number;
}
interface AnchorConfigBase {
  horizontal: RateAndOffset;
  vertical: RateAndOffset;
}
export interface AnchorConfig {
  left: AnchorConfigBase;
  top: AnchorConfigBase;
  buttom: AnchorConfigBase;
  right: AnchorConfigBase;
}
export interface GraphEdge {
  id: string;
  sourceId: string;
  targetId: string;
  source: Point;
  target: Point;
  nextLinkShape: 'smooth' | 'broken';
}
export type DefaultProps = {
  innerClassName?: string;
  childrenClassName?: string;
  nodeClassClassName?: string;
};
export interface NodeDefined {
  component: React.FC<any>;
  props?: DefaultProps & Record<string, any>;
  nextLinkShape?: 'smooth' | 'broken';
  anchorConfig?: AnchorConfig;
  operator?: React.ReactChild;
}
export type SelectHandler = (node: GraphTreeValue) => void;
export interface GraphTreeTemplateProps {
  node: GraphTreeValue;
  positionHub: PositionHub;
  children?: React.ReactChild;
  Component?: React.FC<NodeDefinedProps>;
  onSelect?: SelectHandler;
  onDoubleSelect?: SelectHandler;
  childrenClassName?: string;
  defaultProps?: DefaultProps;
}
export interface GraphAnchor {
  type: string;
  left: Point;
  top: Point;
  right: Point;
  buttom: Point;
}
export interface GraphPoint {
  lt: Point;
  rt: Point;
  lb: Point;
  rb: Point;
  rightCenter: Point;
  width: number;
  height: number;
  center: Point;
}
export interface GraphCoreProps {
  name: string;
  value: GraphValue[];
  width: string | number;
  height: string | number;
  nodes: Record<string, NodeDefined>;
  canvasClassName?: string;

  onSelect?: SelectHandler;
  onDoubleSelect?: SelectHandler;
  onCanvasClick?: React.MouseEventHandler<HTMLDivElement>;
  onControlMouseWheel?: React.WheelEventHandler<HTMLDivElement>;

  children?: React.ReactNode;
  disabledMoveAndZoom?: boolean;
}
