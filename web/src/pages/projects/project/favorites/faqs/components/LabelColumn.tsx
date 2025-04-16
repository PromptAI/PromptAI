import { Space, Tag } from '@arco-design/web-react';
import { isEmpty } from 'lodash';
import React from 'react';

const LabelColumn = ({ item, emptyTitle }) => {
  return isEmpty(item?.user?.data?.labels) ? (
    <span>{emptyTitle}</span>
  ) : (
    <Space wrap>
      {item?.user?.data?.labels.map((l) => (
        <Tag color={'arcoblue'} key={l.text}>
          {l.text}
        </Tag>
      ))}
    </Space>
  );
};

export default LabelColumn;
