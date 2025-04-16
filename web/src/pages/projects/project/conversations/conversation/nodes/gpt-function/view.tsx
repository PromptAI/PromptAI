import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';
import IconText from '@/graph-next/components/IconText';

const FunctionView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['function-gpt'], { className: 'app-icon' }),
    []
  );

  return <IconText icon={icon}>{props.data.name}</IconText>;
};
export default FunctionView;
