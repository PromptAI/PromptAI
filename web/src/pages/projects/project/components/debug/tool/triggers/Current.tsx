import { useProjectContext } from '@/layout/project-layout/context';
import { Button } from '@arco-design/web-react';
import React from 'react';
import { DebugRunProps } from './types';

const DebugCurrent = ({ title, icon, current, start }: DebugRunProps) => {
  const { id: projectId } = useProjectContext();
  return (
    <Button type="text" icon={icon} onClick={() => start([current], projectId)}>
      {title}
    </Button>
  );
};

export default DebugCurrent;
