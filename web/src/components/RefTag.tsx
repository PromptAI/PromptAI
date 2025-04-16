import React from 'react';
import { Tag, TagProps } from '@arco-design/web-react';

type RefTagProps = TagProps;
const RefTag: React.FC<RefTagProps> = ({ style, children, ...props }) => {
  return (
    <Tag
      color="blue"
      size="small"
      style={{
        maxWidth: 220,
        display: 'block',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        ...style,
      }}
      title={typeof children === 'string' ? children : ''}
      {...props}
    >
      {children}
    </Tag>
  );
};

export default RefTag;
