import * as React from 'react';
import { SummaryWithDateRangeProps } from '../types';
import useUrlParams from '../../hooks/useUrlParams';
import { useRequest } from 'ahooks';
import { IParamsWithPage, fallback } from '@/api/statistics';
import { Button, Card, Modal, Table, TableProps } from '@arco-design/web-react';
import dayjs from 'dayjs';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';

interface FallbackProps extends SummaryWithDateRangeProps {}
const Fallback: React.FC<FallbackProps> = ({ startTime, endTime }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [pageParams, setPageParams] = React.useState<
    Pick<IParamsWithPage, 'page' | 'size'>
  >({ page: 0, size: 5 });
  const { loading, data } = useRequest(
    () => fallback({ projectId, startTime, endTime, ...pageParams }),
    {
      refreshDeps: [pageParams, startTime, endTime, projectId],
    }
  );
  const columns = React.useMemo<TableProps['columns']>(
    () => [
      { dataIndex: 'messageId', title: t['fallback.table.messageId'] },
      { dataIndex: 'chatId', title: t['fallback.table.chatId'] },
      { dataIndex: 'query', title: t['fallback.table.query'] },
      {
        dataIndex: 'time',
        title: t['fallback.table.time'],
        render: (_, row) =>
          dayjs(Number(row.time)).format('YYYY-MM-DD HH:mm:ss'),
      },
    ],
    [t]
  );
  const [visible, setVisible] = React.useState(false);
  React.useEffect(() => {
    if (visible) {
      setPageParams({ page: 0, size: 5 });
    }
  }, [visible]);
  return (
    <Card
      title={t['fallback.title']}
      size="small"
      extra={
        <Button size="small" type="text" onClick={() => setVisible(true)}>
          {t['more']}
        </Button>
      }
    >
      <Table
        size="small"
        border={false}
        loading={loading}
        rowKey="messageId"
        columns={columns}
        data={data?.data}
        pagination={false}
      />
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        title={t['more']}
        unmountOnExit
        footer={(cancel) => cancel}
        style={{ width: '50%' }}
      >
        <Table
          size="small"
          border={{
            cell: true,
            wrapper: true,
          }}
          loading={loading}
          data={data?.data}
          columns={columns}
          pagePosition="bl"
          pagination={{
            total: data?.totalCount || 0,
            current: pageParams.page + 1,
            pageSize: pageParams.size,
            onChange: (page, size) =>
              setPageParams({ page: page - 1 < 0 ? 0 : page - 1, size }),
            sizeCanChange: true,
            showTotal: true,
            size: 'mini',
            sizeOptions: [50, 100, 200, 300],
          }}
        />
      </Modal>
    </Card>
  );
};

export default Fallback;
