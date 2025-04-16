import { Tooltip } from '@arco-design/web-react';
import React, { useMemo } from 'react';

const UseSceneColumn = ({ row }) => {
  const last = useMemo(
    () =>
      row.properties.useScene
        ? row.properties.useScene[row.properties.useScene.length - 1]
        : '-',
    [row.properties.useScene]
  );
  const content = useMemo(
    () => (row.properties.useScene || ['-']).join(','),
    [row.properties.useScene]
  );

  return <Tooltip content={content}>{last}</Tooltip>;
};

export default UseSceneColumn;
