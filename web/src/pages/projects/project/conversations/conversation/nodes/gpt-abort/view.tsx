import { nodeIconsMap } from '../config';
import React, { cloneElement, useMemo } from 'react';
import IconText from '@/graph-next/components/IconText';

const AbortView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['abort-gpt'], { className: 'app-icon' }),
    []
  );

  return <IconText icon={icon}>Abort</IconText>;
};

export default AbortView;
