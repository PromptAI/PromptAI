import { dashboardEvaluate } from '@/api/dashboard';
import { useProjectContext } from '@/layout/project-layout/context';
import { Select, Space, Spin, Typography } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import React, { useEffect, useRef, useState } from 'react';
import DashCard from './DashCard';
import { buildParams } from '../util';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import * as echart from 'echarts';

const Evaluate = () => {
  const { id } = useProjectContext();
  const [time, setTime] = useState('7');
  const { loading, data } = useRequest(
    () => dashboardEvaluate(buildParams(time, { projectId: id })),
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
            trigger: 'item',
          },
          legend: {
            top: '5%',
            left: 'center',
          },
          series: [
            {
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2,
              },
              label: {
                show: false,
                position: 'center',
              },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 16,
                  fontWeight: 'bold',
                },
              },
              labelLine: {
                show: false,
              },
              data: data.map((d) => ({
                value: d.count,
                name: t[`dashboard.evaluate.${d.name}`],
              })),
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
            {t['dashboard.evaluate.name']}
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
          {t['dashboard.evaluate.count']}:{' '}
          {data?.reduce((p, c) => p + Number(c.count), 0)}
        </Typography.Title>
        <div ref={ref} style={{ width: '100%', height: 320 }} />
      </Spin>
    </DashCard>
  );
};

export default Evaluate;
