import { Typography } from '@arco-design/web-react';
import { get } from 'lodash';
import React, { useMemo } from 'react';
import type { TimeColumnProps } from './type';
import dayjs from 'dayjs';

const TimeColumn = ({ row, dataIndex, ...rest }: TimeColumnProps) => {
  const time = useMemo(() => {
    const objTime = get(row, dataIndex);
    if (objTime && objTime !== '0') {
      return dayjs(Number(objTime)).format('YYYY-MM-DD HH:mm:ss');
    }
    return '-';
  }, [row, dataIndex]);
  return <Typography.Text {...rest}>{time}</Typography.Text>;
};

export default TimeColumn;
