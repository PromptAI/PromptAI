import * as React from 'react';
import {
  Button,
  Card,
  Grid,
  Modal,
  Table,
  TableProps,
} from '@arco-design/web-react';
import { SummaryWithDateRangeProps } from '../types';
import { useRequest } from 'ahooks';
import { IParamsWithPage, faq } from '@/api/statistics';
import useUrlParams from '../../hooks/useUrlParams';
import i18n from './i18n';
import useLocale from '@/utils/useLocale';
import styled from 'styled-components';
import * as echart from 'echarts';
import { sum } from 'lodash';

const ChartContainer = styled.div`
  height: 246px;
  border: 1px solid var(--color-border-2);
  padding: 4px;
  border-radius: 4px;
`;
interface FaqProps extends SummaryWithDateRangeProps {}
const Faq: React.FC<FaqProps> = ({ startTime, endTime, summary }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [pageParams, setPageParams] = React.useState<
    Pick<IParamsWithPage, 'page' | 'size'>
  >({ page: 0, size: 5 });
  const { loading, data } = useRequest(
    () => faq({ projectId, startTime, endTime, ...pageParams }),
    {
      refreshDeps: [pageParams, startTime, endTime, projectId],
    }
  );
  const columns = React.useMemo<TableProps['columns']>(
    () => [
      {
        dataIndex: 'query',
        title: t['faq.table.query'],
      },
      {
        dataIndex: 'count',
        title: t['faq.table.count'],
        align: 'center',
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

  const ref = React.useRef<HTMLDivElement>();
  React.useEffect(() => {
    const chart = echart.init(ref.current);
    loading ? chart.showLoading() : chart.hideLoading();
    if (data?.data) {
      const total = sum(data.data.map(({ count }) => Number(count)));
      const value = summary?.faq ? Number(summary.faq) - total : 0;
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
            data: [
              ...data.data.map(({ count, query: name }) => ({
                name,
                value: Number(count),
              })),
              { name: t['other'], value },
            ],
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
    }
    const resize = new ResizeObserver(() =>
      chart.resize({ animation: { duration: 500, easing: 'linear' } })
    );
    resize.observe(ref.current);
    return () => {
      chart.dispose();
      resize.disconnect();
    };
  }, [data, loading, t, summary]);

  return (
    <Grid.Row gutter={16}>
      <Grid.Col span={16}>
        <Card
          title={t['faq.title']}
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
            rowKey="query"
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
              rowKey="query"
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
      </Grid.Col>
      <Grid.Col span={8}>
        <ChartContainer ref={ref} />
      </Grid.Col>
    </Grid.Row>
  );
};

export default Faq;
