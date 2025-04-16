import toShrinkNumber from '@/utils/toShakingNumber';
import { Typography, TypographyTextProps } from '@arco-design/web-react';
import { get } from 'lodash';
import React, { useMemo } from 'react';

interface ShrinkSizeColumnProps extends TypographyTextProps {
  row: any;
  dataIndex: string;
}
const ShrinkSizeColumn = ({
  row,
  dataIndex,
  ...rest
}: ShrinkSizeColumnProps) => {
  const size = useMemo(() => {
    const s = get(row, dataIndex);
    return s ? toShrinkNumber(s) : '-';
  }, [row, dataIndex]);
  return <Typography.Text {...rest}>{size}</Typography.Text>;
};

export default ShrinkSizeColumn;
