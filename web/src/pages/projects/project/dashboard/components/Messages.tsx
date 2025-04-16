import { dashboardMessages } from '@/api/dashboard';
import { useProjectContext } from '@/layout/project-layout/context';
import { Select, Space, Typography } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import React, { useEffect, useRef, useState } from 'react';
import DashCard from './DashCard';
import { buildParams } from '../util';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import * as echart from 'echarts';

const Messages = () => {
  const { id } = useProjectContext();
  const [time, setTime] = useState('7');
  const { loading, data } = useRequest(
    () => dashboardMessages(buildParams(time, { projectId: id })),
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
              type: 'bar',
              name: t['dashboard.message.total'],
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
            {t['dashboard.message.total']}
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
      <div ref={ref} style={{ width: '100%', height: 320 }} />
    </DashCard>
  );
};

export default Messages;
