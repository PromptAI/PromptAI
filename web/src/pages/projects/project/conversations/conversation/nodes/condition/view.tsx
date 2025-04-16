import IconText from '@/graph-next/components/IconText';
import useLocale from '@/utils/useLocale';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';
import i18n from '../locale';

const ConditionView = (props) => {
  const t = useLocale(i18n);
  const icon = useMemo(
    () =>
      cloneElement(nodeIconsMap['condition'], {
        className: 'app-icon',
        color: props?.data?.color,
      }),
    [props?.data?.color]
  );
  return (
    <IconText icon={icon} color={props.data?.color}>
      {props.data?.name || t['flow.node.condition']}
    </IconText>
  );
};

export default ConditionView;
