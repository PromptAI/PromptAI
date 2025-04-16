import IconText from '@/graph-next/components/IconText';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';

const FlowView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['flow'], { className: 'app-icon' }),
    []
  );
  return <IconText icon={icon}>{props.data.name}</IconText>;
};

export default FlowView;
