import { Grid, Typography } from '@arco-design/web-react';
import React, { CSSProperties, PropsWithChildren } from 'react';

const style: CSSProperties = {
  padding: 16,
  display: 'flex',
  flexDirection: 'column',
  height: '100%',
  border: '1px solid rgb(var(--gray-2))',
  borderTop: 'none',
};
interface PageContainerProps {
  title: React.ReactNode;
}
const PageContainer = ({
  title,
  children,
}: PropsWithChildren<PageContainerProps>) => {
  return (
    <div style={style}>
      <Grid.Row>
        <Grid.Col>
          <Typography.Title heading={5}>{title}</Typography.Title>
        </Grid.Col>
      </Grid.Row>
      <Grid.Row style={{ flex: 1 }}>
        <Grid.Col span={24}>{children}</Grid.Col>
      </Grid.Row>
    </div>
  );
};

export default PageContainer;
