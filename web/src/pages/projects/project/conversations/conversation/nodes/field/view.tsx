import IconText from '@/graph-next/components/IconText';
import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '../config';

const FieldView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['field'], { className: 'app-icon' }),
    []
  );
  return (
    <IconText icon={icon}>{props.data?.slotDisplay || 'Undefined'}</IconText>
  );
};

export default FieldView;
