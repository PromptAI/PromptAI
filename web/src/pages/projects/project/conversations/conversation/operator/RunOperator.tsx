import { useDefaultLocale } from '@/utils/useLocale';
import { Button } from '@arco-design/web-react';
import { IconLoading, IconPlayCircleFill } from '@arco-design/web-react/icon';
import React from 'react';

interface RunOperatorProps {
  onClick: () => void;
  disabled: any;
}
const RunOperator = ({ onClick, disabled }: RunOperatorProps) => {
  const dt = useDefaultLocale();
  const iconType = {
    loading_running: <IconLoading />,
    display_running: <IconPlayCircleFill />,
    true: <IconLoading />,
    false: <IconPlayCircleFill />,
  };
  return (
    <Button
      type="text"
      disabled={disabled}
      onClick={onClick}
      status="success"
      icon={disabled ? iconType[disabled] : <IconPlayCircleFill />}
    >
      {!!disabled ? dt['command.trian.running'] : dt['command.trian.run']}
    </Button>
  );
};

export default RunOperator;
