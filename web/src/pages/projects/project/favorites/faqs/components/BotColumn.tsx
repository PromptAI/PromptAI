import { Tooltip } from '@arco-design/web-react';
import React from 'react';

const BotColumn = ({ item }) => {
  return (
    <Tooltip
      position={'tl'}
      content={item?.bot?.data?.responses?.[0].content?.text || '-'}
    >
      {item?.bot?.data?.responses?.[0].content?.text || '-'}
    </Tooltip>
  );
};

export default BotColumn;
