import IconText from '@/graph-next/components/IconText';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';

const FormView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['form'], { className: 'app-icon' }),
    []
  );
  return <IconText icon={icon}>{props.data.name}</IconText>;
};

export default FormView;
