import { listIntents } from '@/api/components';
import { infoProject } from '@/api/projects';
import clipboard from '@/utils/clipboard';
import {
  Button,
  Card,
  Grid,
  Message,
  Table,
  TableColumnProps,
} from '@arco-design/web-react';
import { IconCopy, IconSync } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router';

const { Row, Col } = Grid;
const columns: TableColumnProps[] = [
  {
    title: 'Text',
    key: 'text',
    ellipsis: true,
    render(col, item) {
      return item?.data?.examples && item?.data?.examples[0];
    },
  },
  {
    title: 'Option',
    key: 'option',
    align: 'center',
    width: 80,
    render(col, item) {
      const copy = () => {
        clipboard(item?.data?.examples[0]).then(() =>
          Message.success('Copy Success')
        );
      };
      return <IconCopy onClick={copy} />;
    },
  },
];
const Run = () => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_, setsendMessageHeader] = useState<any>();

  const { id: projectId } = useParams<{ id: string }>();
  const {
    loading,
    data = [],
    refresh,
  } = useRequest(() => listIntents(projectId), {
    refreshDeps: [projectId],
  });

  useEffect(() => {
    const requset = async () => {
      const { debugProject } = await infoProject(projectId);
      if (debugProject) {
        const { id, token } = debugProject;
        setsendMessageHeader({
          'X-published-project-id': id,
          'X-published-project-token': token,
          'X-project-id': projectId,
        });
      }
    };
    requset();
  }, [projectId]);

  return (
    <div style={{ marginLeft: 7, marginRight: 7 }}>
      <Row gutter={14}>
        <Col span={16}>
          <Card
            size="small"
            title="Question"
            bordered={false}
            extra={<Button type="text" icon={<IconSync />} onClick={refresh} />}
          >
            <Table
              borderCell
              size="small"
              loading={loading}
              rowKey="id"
              data={data}
              columns={columns}
              scroll={{
                y: 330,
              }}
              pagination={{
                size: 'mini',
                sizeCanChange: true,
                showTotal: true,
                total: data?.length,
                defaultPageSize: 20,
                pageSizeChangeResetCurrent: true,
              }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small" title="Robot" bordered={false}>
            {/* <Robot
              header={sendMessageHeader}
              sender={sender}
              width="100%"
              height="calc(100vh - 180px)"
              background="rgb(var(--gray-2))"
            /> */}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Run;
