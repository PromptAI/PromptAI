import useLocale from '@/utils/useLocale';
import { Tooltip } from '@arco-design/web-react';
import React from 'react';
import i18n from './locale';
import TrainLib, { TrainLibProps } from './TrainLib';

const TooltipTrainLib = ({
  tooltipDisable,
  ...props
}: TrainLibProps & { tooltipDisable?: boolean }) => {
  const t = useLocale(i18n);
  return (
    <Tooltip disabled={!tooltipDisable} content={t['train.tooltip.need.sync']}>
      <div>
        <TrainLib {...props} />
      </div>
    </Tooltip>
  );
};

export default TooltipTrainLib;
