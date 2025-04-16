import * as React from 'react';
import i18n from './locale';
import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { useMutation } from 'react-query';
import useUrlParams from '../../../hooks/useUrlParams';
import { IconDelete } from '@arco-design/web-react/icon';
import { batchDelelteFile } from '@/api/text/pdf';

interface BatchDeleteProps {
  ids: string[];
  onSuccess: () => void;
}
const BatchDelete: React.FC<BatchDeleteProps> = ({ ids, onSuccess }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { isLoading, mutate } = useMutation(batchDelelteFile, {
    onSuccess: () => {
      Message.success(t['batch.delete.success']);
      onSuccess();
    },
  });
  return (
    <Button
      type="outline"
      status="danger"
      loading={isLoading}
      onClick={() => mutate({ projectId, ids })}
      icon={<IconDelete />}
    >
      {t['batch.delete']}({ids.length})
    </Button>
  );
};

export default BatchDelete;
