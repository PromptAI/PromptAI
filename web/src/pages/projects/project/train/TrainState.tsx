import useLocale from '@/utils/useLocale';
import {
  Card,
  Divider,
  Tag,
  Tooltip,
  Trigger,
  Typography,
} from '@arco-design/web-react';
import {
  IconCheckCircle,
  IconCloseCircle,
  IconLoop,
} from '@arco-design/web-react/icon';
import moment from 'moment';
import React, { useMemo } from 'react';
import i18n from './locale';

const STATUS_ICON = {
  running: <IconLoop style={{ color: '#165DFF' }} spin />,
  failed: <IconCloseCircle style={{ color: '#F53F3F' }} color="#" />,
  success: <IconCheckCircle style={{ color: '#0ea5e9' }} color="#" />,
};

const STATUS_COLOR = {
  running: '#165DFF',
  failed: '#F53F3F',
  success: '#0ea5e9',
};

const Popup = ({ item }) => {
  const { status, createByName, properties } = item;
  const t = useLocale(i18n);
  const STATE_TEXT = useMemo(
    () => ({
      running: t['train.publishing'],
      failed: t['train.publishing.failed'],
      success: t['train.publishing.complete'],
    }),
    [t]
  );
  const errors = useMemo(
    () =>
      properties?.stages?.flatMap((s) =>
        s.status === 'failed' ? [s.lastMessage] : []
      ) || [],
    [properties?.stages]
  );

  return (
    <Card size="small">
      <div className="flex flex-col space-y-2">
        <div className="space-x-2">
          <Tooltip className="border-none" color={STATUS_COLOR[status]}>
            <Tag color={STATUS_COLOR[status]}>{STATE_TEXT[status]}</Tag>
          </Tooltip>
          <Typography.Text>
            {moment(Number(properties?.createTime)).format(
              'YYYY-MM-DD HH:mm:ss'
            )}
          </Typography.Text>
        </div>
        <Typography.Text>
          {t['train.deploy']}: {createByName}
        </Typography.Text>
        <Divider />
        <ol className="max-w-md list-disc list-inside space-y-1">
          {errors.map((e, index) => (
            <li key={index} className="marker:text-[rgb(var(--danger-6))]">
              {e}
            </li>
          ))}
        </ol>
      </div>
    </Card>
  );
};

export default function TrainState({ data = [] }) {
  return (
    <div className="px-1 flex items-center text-lg gap-2">
      {data.length > 0 &&
        data
          .sort(
            (a, b) =>
              Number(a.properties.createTime) - Number(b.properties.createTime)
          )
          .map((item) => (
            <Trigger
              key={item.id}
              popup={() => <Popup item={item} />}
              position="bottom"
            >
              <div className="cursor-pointer">
                {STATUS_ICON[item.status] || '-'}
              </div>
            </Trigger>
          ))}
    </div>
  );
}
