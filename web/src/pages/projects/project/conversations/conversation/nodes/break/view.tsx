import IconText from '@/graph-next/components/IconText';
import useLocale from '@/utils/useLocale';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';
import i18n from '../locale';

const BreakView = (props) => {
  const t = useLocale(i18n);
  const icon = useMemo(
    () =>
      cloneElement(nodeIconsMap['break'], {
        className: 'app-icon',
        style: { color: props?.data?.color },
      }),
    [props?.data?.color]
  );
  return (
    <IconText icon={icon} color={props.data?.color}>
      {props.data?.name || t['flow.node.break']}
    </IconText>
  );
};

export default BreakView;
