import { Card, CardProps } from '@arco-design/web-react';
import React from 'react';

const DashCard = ({ children, ...props }: CardProps) => {
  return (
    <Card
      size="small"
      headerStyle={{ height: 48, background: '#D6C2EB', borderRadius: 8 }}
      style={{ borderTopLeftRadius: 8, borderTopRightRadius: 8 }}
      bordered={false}
      {...props}
    >
      {children}
    </Card>
  );
};

export default DashCard;
