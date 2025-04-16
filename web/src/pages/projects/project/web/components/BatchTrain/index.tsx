import * as React from 'react';
import { Button, ButtonProps } from '@arco-design/web-react';
import useUrlParams from '../../../hooks/useUrlParams';
import useLocale from '@/utils/useLocale';
import i18n from './locale';
import { useMutation } from 'react-query';
import { batchTrainWeb } from '@/api/text/train';

interface BatchTrainProps extends ButtonProps {
  ids: string[];
  onSuccess?: () => void;
}
const BatchTrain: React.FC<BatchTrainProps> = ({
  ids,
  onSuccess,
  children,
  ...buttonProps
}) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { isLoading, mutate } = useMutation(batchTrainWeb, { onSuccess });

  return (
    <Button
      {...buttonProps}
      loading={isLoading}
      onClick={() => mutate({ projectId, ids })}
    >
      {children || `${t['batch.train']}(${ids.length})`}
    </Button>
  );
};

export default BatchTrain;
