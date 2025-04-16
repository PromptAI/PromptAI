import * as React from 'react';
import { DateRangeProps, SummaryWithDateRangeProps } from '../types';
import useUrlParams from '../../hooks/useUrlParams';
import { useRequest } from 'ahooks';
import { IParamsWithPage, flow, flowExport, flowInfo } from '@/api/statistics';
import {
  Button,
  Card,
  Grid,
  Modal,
  Space,
  Table,
  TableProps,
} from '@arco-design/web-react';
import { downloadFile } from '@/utils/downloadObject';
import i18n from './i18n';
import useLocale from '@/utils/useLocale';
import styled from 'styled-components';
import * as echart from 'echarts';
import TimeColumn from '@/components/TimeColumn';

interface ExportFlowProps extends DateRangeProps {
  row: any;
}
const ExportFlow: React.FC<ExportFlowProps> = ({ startTime, endTime, row }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { loading, run } = useRequest(
    () => flowExport({ projectId, startTime, endTime, flowId: row.id }),
    {
      manual: true,
      onSuccess: (response) => downloadFile(response),
    }
  );
  return (
    <Button
      type="text"
      status="warning"
      size="mini"
      loading={loading}
      onClick={() => run()}
    >
      {t['export']}
    </Button>
  );
};

interface InfoRowProps extends DateRangeProps {
  row: any;
}
const InfoRow: React.FC<InfoRowProps> = ({ startTime, endTime, row }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [visible, setVisible] = React.useState(false);
  const [pageParams, setPageParams] = React.useState<
    Pick<IParamsWithPage, 'page' | 'size'>
  >({ page: 0, size: 5 });
  const { loading, data, run } = useRequest(
    () =>
      flowInfo({
        projectId,
        startTime,
        endTime,
        flowId: row.id,
        ...pageParams,
      }),
    {
      manual: true,
      refreshDeps: [pageParams, startTime, endTime, row.id, projectId],
    }
  );
  React.useEffect(() => {
    visible && run();
  }, [run, visible]);

  const columns = React.useMemo<TableProps['columns']>(
    () => [
      { dataIndex: 'ip', title: t['flow.table.ip'] },
      {
        dataIndex: 'visitTime',
        title: t['flow.table.visitTime'],
        render: (_, row) => <TimeColumn row={row} dataIndex="visitTime" />,
      },
      ...Object.values<any>(row.requiredEntities || {}).map(
        ({ id, name, display }) => ({
          dataIndex: `filledSlots.${id}`,
          title: display || name,
          render: (value) => value?.value || '-',
        })
      ),
    ],
    [row.requiredEntities, t]
  );
  return (
    <>
      <Button type="text" size="mini" onClick={() => setVisible(true)}>
        {t['info']}
      </Button>
      <Modal
        visible={visible}
        onCancel={() => setVisible(false)}
        title={t['info']}
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
          rowKey="id"
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
    </>
  );
};

const ChartContainer = styled.div`
  height: 256px;
  border: 1px solid var(--color-border-2);
  padding: 4px;
  border-radius: 4px;
`;
interface FlowProps extends SummaryWithDateRangeProps {}
const Flow: React.FC<FlowProps> = ({ startTime, endTime }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { loading, data = [] } = useRequest(
    () => flow({ projectId, startTime, endTime }),
    {
      refreshDeps: [startTime, endTime, projectId],
    }
  );
  const columns = React.useMemo<TableProps['columns']>(
    () => [
      { dataIndex: 'name', title: t['flow.table.name'] },
      { dataIndex: 'hit', title: t['flow.table.hit'], align: 'center' },
      {
        dataIndex: 'finished',
        title: t['flow.table.finished'],
        align: 'center',
      },
      {
        dataIndex: 'unfinished',
        title: t['flow.table.unfinished'],
        align: 'center',
      },
      {
        key: 'op',
        title: t['operations'],
        align: 'center',
        width: 120,
        render: (_, row) => (
          <Space size="mini">
            <InfoRow startTime={startTime} endTime={endTime} row={row} />
            <ExportFlow startTime={startTime} endTime={endTime} row={row} />
          </Space>
        ),
      },
    ],
    [endTime, startTime, t]
  );
  const ref = React.useRef<HTMLDivElement>();
  React.useEffect(() => {
    const chart = echart.init(ref.current);
    loading ? chart.showLoading() : chart.hideLoading();
    data &&
      chart.setOption({
        tooltip: {
          trigger: 'item',
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [
          {
            name: t['flow.table.hit'],
            type: 'pie',
            radius: '50%',
            data: data.map(({ name, hit }) => ({ name, value: Number(hit) })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
              },
            },
          },
        ],
      });
    const resize = new ResizeObserver(() =>
      chart.resize({ animation: { duration: 500, easing: 'linear' } })
    );
    resize.observe(ref.current);
    return () => {
      chart.dispose();
      resize.disconnect();
    };
  }, [data, loading, t]);

  return (
    <Grid.Row gutter={16}>
      <Grid.Col span={16}>
        <Card title={t['flow.title']} size="small">
          <Table
            size="small"
            border={false}
            loading={loading}
            rowKey="id"
            columns={columns}
            data={data}
            pagination={false}
          />
        </Card>
      </Grid.Col>
      <Grid.Col span={8}>
        <ChartContainer ref={ref} />
      </Grid.Col>
    </Grid.Row>
  );
};

export default Flow;
