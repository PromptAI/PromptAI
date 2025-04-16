import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';
import IconText from '@/graph-next/components/IconText';

const GptSlotView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['slot-gpt'], { className: 'app-icon' }),
    []
  );

  return <IconText icon={icon}>{props.data.slotName}</IconText>;
};

export default GptSlotView;
