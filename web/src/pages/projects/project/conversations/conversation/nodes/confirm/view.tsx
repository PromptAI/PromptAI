import IconText from '@/graph-next/components/IconText';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';

const ConfirmView = () => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['confirm'], { className: 'app-icon' }),
    []
  );
  return <IconText icon={icon}>Confirm</IconText>;
};

export default ConfirmView;
