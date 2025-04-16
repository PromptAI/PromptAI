import * as React from 'react';
import { GraphEdge, GraphTreeValue } from './types';
import { treeForEach } from './utils';
import SmoothEdge from './SmoothEdge';
import PositionHub from './PositionHub';

const computeEdgeLinks = (tree, positionHub: PositionHub) => {
  const edgesLinks: GraphEdge[] = [];
  treeForEach(tree, (node) => {
    if (node.parentId) {
      const { parentId, id } = node;
      const sourceAnchor = positionHub.get(parentId);
      const targetAnchor = positionHub.get(id);

      if (sourceAnchor && targetAnchor) {
        const source = sourceAnchor.right;
        const target = targetAnchor.left;
        edgesLinks.push({
          id: `${parentId}&${id}`,
          sourceId: parentId,
          targetId: id,
          source,
          target,
          nextLinkShape: 'smooth',
        });
      }
    }
  });
  return edgesLinks;
};

export default function useGraphEdge(
  treeValue: GraphTreeValue[],
  positionHub: PositionHub
) {
  const [edges, setEdges] = React.useState<GraphEdge[]>([]);
  React.useEffect(() => {
    setTimeout(() => {
      setEdges(computeEdgeLinks(treeValue, positionHub));
    });
  }, [treeValue, positionHub]);
  return {
    edges,
    edgeComponent: edges.map((edge) =>
      React.createElement(SmoothEdge, { key: edge.id, edge: edge })
    ),
  };
}
