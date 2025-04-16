import IconText from '@/graph-next/components/IconText';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';

const InterruptView = () => {
  const icon = useMemo(
    () =>
      cloneElement(nodeIconsMap['interrupt'], {
        className: 'app-icon',
        style: { fontSize: 16 },
      }),
    []
  );
  return <IconText icon={icon}>Interrupts</IconText>;
};

export default InterruptView;
