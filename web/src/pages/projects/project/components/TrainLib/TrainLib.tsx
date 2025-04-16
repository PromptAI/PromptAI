import { trainLib } from '@/api/text/train';
import useLocale from '@/utils/useLocale';
import { Button, ButtonProps, Message } from '@arco-design/web-react';
import { useRequest } from 'ahooks';
import React from 'react';
import useUrlParams from '../../hooks/useUrlParams';
import i18n from './locale';

export interface TrainLibProps extends ButtonProps {
  componentId: string;
  onSuccess: () => void;
}
const TrainLib = ({
  componentId,
  onSuccess,
  disabled,
  ...rest
}: Omit<TrainLibProps, 'loading' | 'onClick'>) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { loading, run } = useRequest(() => trainLib(projectId, componentId), {
    refreshDeps: [componentId, projectId],
    onSuccess: () => {
      Message.success(t['train.success']);
      onSuccess();
    },
    manual: true,
  });
  return (
    <Button
      type="primary"
      {...rest}
      loading={loading}
      onClick={() => !disabled && run()}
      style={disabled ? { opacity: 0.5, cursor: 'not-allowed' } : null}
    >
      {t['train.title']}
    </Button>
  );
};

export default TrainLib;
