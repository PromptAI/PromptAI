import { Grid } from '@arco-design/web-react';
import React from 'react';
import Branch from './components/Branch';
import Evaluate from './components/Evaluate';
import Total from './components/Total';
import Messages from './components/Messages';
import MessagesFallback from './components/MessagesFallback';
import Fallbacks from './components/Fallbacks';

const { Row, Col } = Grid;
const Dashboard = () => {
  return (
    <div
      style={{ margin: 8, padding: 8, background: 'var(--color-neutral-1)' }}
    >
      <Row gutter={8}>
        <Col span={8}>
          <Total />
        </Col>
        <Col span={8}>
          <Evaluate />
        </Col>
        <Col span={8}>
          <Branch />
        </Col>
      </Row>
      <Row gutter={8} style={{ marginTop: 8 }}>
        <Col span={8}>
          <Messages />
        </Col>
        <Col span={8}>
          <Fallbacks />
        </Col>
        <Col span={8}>
          <MessagesFallback />
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
