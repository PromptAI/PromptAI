import { Space } from '@arco-design/web-react';
import { IconRobot } from '@arco-design/web-react/icon';
import React from 'react';

const IpColumn = ({ item }) => {
  return (
    <Space>
      <IconRobot />
      {item.properties.ip || '-'}
    </Space>
  );
};

export default IpColumn;
