import React, { cloneElement, useMemo } from 'react';
import { nodeIconsMap } from '@/pages/projects/project/conversations/conversation/nodes/config';
import IconText from '@/graph-next/components/IconText';

const GptSlotsView = (props) => {
  const icon = useMemo(
    () => cloneElement(nodeIconsMap['slots-gpt'], { className: 'app-icon' }),
    []
  );

  return <IconText icon={icon}>Slots</IconText>;
};

export default GptSlotsView;
