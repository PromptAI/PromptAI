import React, { useEffect, useMemo } from 'react';
import { Progress, Space } from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

interface StepProgressProps {
  totalTime?: number;
  status?: 'success' | 'error' | 'normal' | 'warning';
  name: string;
}

const StepProgress = ({
  totalTime = 60 * 1000,
  status,
  name,
}: StepProgressProps) => {
  const t = useLocale(i18n);
  const [value, setValue] = React.useState(0);
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (value < 99) {
      timer = setTimeout(() => {
        setValue(value + 1);
      }, totalTime / 100);
    }
    return () => {
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, [totalTime, value]);
  return (
    <Space size="mini" direction="vertical" className="w-full">
      <Progress percent={value} status={status} style={{ width: 200 }} />
      {value === 99 && (
        <div
          className="flex justify-center"
          style={{ maxWidth: 200 }}
        >{`${name},${t['debug.step.process.timeout.subfix']}`}</div>
      )}
    </Space>
  );
};

interface BackendStepProgressProps {
  value?: number | string;
  status?: 'success' | 'error' | 'normal' | 'warning';
  name: string;
  usedInSec?: number;
  avgInSec?: number | string;
}
const BackendStepProgress = ({
  value,
  status,
  name,
  usedInSec,
  avgInSec,
}: BackendStepProgressProps) => {
  const t = useLocale(i18n);
  const percent = useMemo(
    () => Number(`${value || '0'}`.replace('%', '')),
    [value]
  );
  return (
    <Space
      size="mini"
      direction="vertical"
      className="w-full"
      style={{ height: 120 }}
    >
      <div className="flex" style={{ maxWidth: 200 }}>{`${
        t['debug.step.process.avgPrefix']
      }${avgInSec || 0}${t['debug.step.process.time.unit']}`}</div>
      <Progress
        percent={percent}
        status={status}
        style={{ width: 200 }}
        formatText={(p) =>
          `${p} ${t['debug.step.process.totalPrefix']}${usedInSec || 0}${
            t['debug.step.process.time.unit']
          }`
        }
      />
      {percent === 99 && (
        <div
          className="flex justify-center"
          style={{ maxWidth: 200 }}
        >{`${name},${t['debug.step.process.timeout.subfix']}`}</div>
      )}
    </Space>
  );
};

export { BackendStepProgress };
export default StepProgress;
