import { dashboardTotal } from '@/api/dashboard';
import { useProjectContext } from '@/layout/project-layout/context';
import { Select, Space, Spin, Typography } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import React, { useEffect, useRef, useState } from 'react';
import DashCard from './DashCard';
import { buildParams } from '../util';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import * as echart from 'echarts';

const Total = () => {
  const { id } = useProjectContext();
  const [time, setTime] = useState('7');
  const { loading, data } = useRequest(
    () => dashboardTotal(buildParams(time, { projectId: id })),
    {
      refreshDeps: [time, id],
    }
  );
  const t = useLocale(i18n);
  const ref = useRef<HTMLDivElement>();
  useEffect(() => {
    const chart = echart.init(ref.current);
    if (loading) {
      chart.showLoading();
    } else {
      chart.hideLoading();
      if (data) {
        chart.setOption({
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow',
            },
          },
          xAxis: {
            type: 'category',
            data: data.map((d) => d.day),
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
          },
          series: [
            {
              data: data.map((d) => d.count),
              type: 'line',
              name: t['dashboard.count'],
            },
          ],
        });
      }
    }
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
    <DashCard
      title={
        <Space>
          <Typography.Text type="secondary">
            {t['dashboard.total.name']}
          </Typography.Text>
        </Space>
      }
      extra={
        <Select
          value={time}
          bordered={false}
          style={{ color: 'var(--color-text-2)' }}
          onChange={setTime}
        >
          <Select.Option value="7">7{t['dashboard.day.unit']}</Select.Option>
          <Select.Option value="30">30{t['dashboard.day.unit']}</Select.Option>
          <Select.Option value="90">90{t['dashboard.day.unit']}</Select.Option>
        </Select>
      }
    >
      <Spin loading={loading} style={{ width: '100%' }}>
        <Typography.Title heading={6} type="secondary">
          {t['dashboard.total.count']}:{' '}
          {data?.reduce((p, c) => p + Number(c.count), 0)}
        </Typography.Title>
        <div ref={ref} style={{ width: '100%', height: 320 }} />
      </Spin>
    </DashCard>
  );
};

export default Total;
