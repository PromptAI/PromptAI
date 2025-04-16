import { useCreation } from 'ahooks';
import { cloneDeep, Dictionary, isEmpty, keyBy } from 'lodash';
import React from 'react';
import {
  GraphTreeTemplateProps,
  GraphTreeValue,
  GraphValue,
  NodeDefined,
  SelectHandler,
} from './types';
import PositionHub from './PositionHub';

export function buildGraphTreeValue(value: GraphValue[]) {
  if (!value) return [];
  const tars: GraphTreeValue[] = cloneDeep(value);
  const res: GraphTreeValue[] = [];
  const idsmap: Dictionary<GraphTreeValue> = keyBy(tars, (n) => n.id);
  let node = tars.shift();
  while (node) {
    const mapNode = idsmap[node.parentId];
    if (node.parentId) {
      if (!mapNode) {
        res.push(node);
      } else {
        mapNode.children = mapNode.children || [];
        mapNode.children.push(node);
        mapNode.children = mapNode.children.sort((a, b) => a.no - b.no);
        node.parent = mapNode;
      }
    } else {
      res.push(node);
    }
    node = tars.shift();
  }
  return res;
}

function buildGraphTree(
  treeValues: GraphTreeValue[],
  Template: React.FC<GraphTreeTemplateProps>,
  config: Record<string, NodeDefined>,
  positionHub: PositionHub,
  onSelect?: SelectHandler,
  onDoubleSelect?: SelectHandler
) {
  return treeValues.map((node) => {
    const props = {
      key: node.id,
      node,
      positionHub,
      Component: config[node.type]?.component,
      onSelect,
      onDoubleSelect,
      defaultProps: config[node.type]?.props,
    };
    return isEmpty(node.children)
      ? React.createElement(Template, props)
      : React.createElement(
          Template,
          props,
          ...buildGraphTree(
            node.children,
            Template,
            config,
            positionHub,
            onSelect,
            onDoubleSelect
          )
        );
  });
}

export default function useGraphTree(
  value: GraphValue[],
  Template: React.FC<GraphTreeTemplateProps>,
  config: Record<string, NodeDefined>,
  positionHub: PositionHub,
  onSelect?: SelectHandler,
  onDoubleSelect?: SelectHandler
) {
  return useCreation(() => {
    const treeValue = buildGraphTreeValue(value);
    return {
      treeValue,
      treeComponent: buildGraphTree(
        treeValue,
        Template,
        config,
        positionHub,
        onSelect,
        onDoubleSelect
      ),
    };
  }, [value, Template, config, onSelect]);
}
