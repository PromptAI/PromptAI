import { Space, Tag } from '@arco-design/web-react';
import React from 'react';

const LabelsColumn = ({ row }) => {
  return (
    <Space wrap>
      {row?.user?.data?.labels.map((l) => (
        <Tag color={'arcoblue'} key={l.text}>
          {l.text}
        </Tag>
      ))}
    </Space>
  );
};

export default LabelsColumn;
