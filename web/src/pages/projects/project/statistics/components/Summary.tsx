import * as React from 'react';
import { DateRangeProps, SummaryData } from '../types';
import { useRequest } from 'ahooks';
import { summary } from '@/api/statistics';
import useUrlParams from '../../hooks/useUrlParams';
import { Card, Grid, Spin } from '@arco-design/web-react';
import styled from 'styled-components';
import useLocale from '@/utils/useLocale';
import i18n from './i18n';
import * as echart from 'echarts';

const keys = ['chat', 'message'];
const Text = styled.span`
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-2);
`;
const Content = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 32px;
`;
const ChartContainer = styled.div`
  height: 128px;
  border: 1px solid var(--color-border-2);
  padding: 4px;
  border-radius: 4px;
`;
const summaryWrap = async (...args: Parameters<typeof summary>) => {
  const { hit, ...rest } = await summary(...args);
  return { ...rest, ...hit };
};

interface SummaryProps extends DateRangeProps {
  onChange?: (summary: SummaryData) => void;
}
const Summary: React.FC<SummaryProps> = ({ startTime, endTime, onChange }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { loading, data } = useRequest(
    () => summaryWrap({ projectId, startTime, endTime }),
    { refreshDeps: [startTime, endTime, projectId], onSuccess: onChange }
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
            data: ['knowledgeBase', 'conversation', 'faq', 'fallback'].map(
              (key) => ({
                name: t[`summary.${key}`] + ` : ${Number(data[key])}`,
                value: Number(data[key]),
              })
            ),
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
    <Spin loading={loading} className="w-full">
      <Grid.Row gutter={16}>
        {keys.map((key) => (
          <Grid.Col key={key} span={Math.floor(16 / keys.length)}>
            <Card size="small" style={{ height: 128 }}>
              <Text>{t[`summary.${key}`]}</Text>
              <Content>{data?.[key] || 0}</Content>
            </Card>
          </Grid.Col>
        ))}
        <Grid.Col span={8}>
          <ChartContainer ref={ref} />
        </Grid.Col>
      </Grid.Row>
    </Spin>
  );
};
export default Summary;
