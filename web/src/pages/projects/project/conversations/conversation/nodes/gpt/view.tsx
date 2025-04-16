import { nodeIconsMap } from '../config';
import React, { cloneElement, useMemo } from 'react';
import IconText from '@/graph-next/components/IconText';

const GPTView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['gpt'], { className: 'app-icon' }),
    []
  );

  return <IconText icon={icon}>{props.data.name}</IconText>;
};

export default GPTView;
