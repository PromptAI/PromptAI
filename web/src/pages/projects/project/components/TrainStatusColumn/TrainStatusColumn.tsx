import useLocale from '@/utils/useLocale';
import { Tag, TagProps } from '@arco-design/web-react';
import { get } from 'lodash';
import React, { useMemo } from 'react';
import i18n from './locale';

interface TrainStatusColumnProps extends TagProps {
  row: any;
  dataIndex: string;
}
const TrainStatusColumn = ({
  row,
  dataIndex,
  ...rest
}: Omit<TrainStatusColumnProps, 'color'>) => {
  const t = useLocale(i18n);
  const TRAIN_STATUS = useMemo(
    () => ({
      undefined: { text: t['train.status.undefined'], color: null },
      wait_train: { text: t['train.status.wait'], color: 'gray' },
      wait_parse: { text: t['train.status.wait_parse'], color: 'gray' },
      parsing: { text: t['train.status.parsing'], color: 'gray' },
      training: { text: t['train.status.training'], color: 'blue' },
      finish_train: { text: t['train.status.finish'], color: 'green' },
      parse_fail: { text: t['train.status.parse_fail'], color: 'red' },
    }),
    [t]
  );
  const status = useMemo(
    () => TRAIN_STATUS[get(row, dataIndex)] || TRAIN_STATUS.undefined,
    [TRAIN_STATUS, row, dataIndex]
  );
  return (
    <Tag
      {...rest}
      color={status.color}
      style={{
        maxWidth: 200,
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
      }}
    >
      {status.text}
    </Tag>
  );
};

export default TrainStatusColumn;
