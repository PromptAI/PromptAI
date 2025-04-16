import getNodes from '@/pages/projects/project/conversations/conversation/nodes';
import React, { useMemo } from 'react';
import GraphCore from '@/core-next';
import { nanoid } from 'nanoid';

const config = getNodes({}, true);
const Graph = ({ value }) => {
  return (
    <GraphCore
      name="favorite-flow-graph"
      width="100%"
      height="400px"
      value={value}
      nodes={config}
    />
  );
};
const Detail = ({ nodes }) => {
  const value = useMemo(() => {
    const prefix = nanoid() + '_';
    return nodes.map((f) => {
      return {
        ...f,
        id: prefix + f.id,
        parentId: f.parentId ? prefix + f.parentId : f.parentId,
      };
    });
  }, [nodes]);
  return (
    <div className="w-full">
      <Graph value={value} />
    </div>
  );
};

export default Detail;
