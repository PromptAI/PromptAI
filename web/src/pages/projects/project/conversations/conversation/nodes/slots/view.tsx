import IconText from '@/graph-next/components/IconText';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';

const SlotsView = () => {
  const icon = useMemo(
    () =>
      cloneElement(nodeIconsMap['slots'], {
        className: 'app-icon',
        style: { fontSize: 16 },
      }),
    []
  );

  return <IconText icon={icon}>Slots</IconText>;
};

export default SlotsView;
