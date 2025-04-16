import IconText from '@/graph-next/components/IconText';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';

const FunctionsView = (props) => {
  const icon = useMemo(
    () =>
      cloneElement(nodeIconsMap['functions-gpt'], { className: 'app-icon' }),
    []
  );
  return <IconText icon={icon}>Functions</IconText>;
};

export default FunctionsView;
