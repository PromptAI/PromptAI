import React, { useMemo } from 'react';

const NodeCountColumn = ({ item }) => {
  const count = useMemo(() => item.nodes.length ?? '-', [item]);
  return <span>{count}</span>;
};

export default NodeCountColumn;
