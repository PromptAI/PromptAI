import { nodeIconsMap } from '../config';
import React, { cloneElement, useMemo } from 'react';
import IconText from '@/graph-next/components/IconText';

const CompleteView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['complete-gpt'], { className: 'app-icon' }),
    []
  );

  return <IconText icon={icon}>complete</IconText>;
};
export default CompleteView;
