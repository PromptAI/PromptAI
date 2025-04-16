import { Tooltip } from '@arco-design/web-react';
import React from 'react';

const UserColumn = ({ item }) => {
  return (
    <Tooltip
      position={'tl'}
      content={item?.user?.data?.examples?.[0]?.text || '-'}
    >
      {item?.user?.data?.examples?.[0]?.text || '-'}
    </Tooltip>
  );
};

export default UserColumn;
