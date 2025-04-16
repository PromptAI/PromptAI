import * as React from 'react';
import { batchDeleteComponent } from '@/api/components';
import {
  Button,
  ButtonProps,
  Message,
  Popconfirm,
} from '@arco-design/web-react';
import useUrlParams from '../../hooks/useUrlParams';
import { useMutation } from 'react-query';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

interface DeleteComponentButtonProps extends ButtonProps {
  ids: string[];
  onSuccess?: (data?: any) => void;
}
const DeleteComponentButton: React.FC<DeleteComponentButtonProps> = ({
  ids,
  onSuccess,
  children,
  ...buttonProps
}) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const { isLoading, mutateAsync } = useMutation(batchDeleteComponent, {
    onSuccess: (data) => {
      Message.success(t['delete.success']);
      onSuccess?.(data);
    },
  });
  return (
    <Popconfirm
      focusLock
      title={t['delete.sure']}
      onOk={() => mutateAsync({ projectId, ids })}
    >
      <Button
        type="text"
        size="small"
        status="danger"
        {...buttonProps}
        loading={isLoading}
      >
        {children || t['delete']}
      </Button>
    </Popconfirm>
  );
};

export default DeleteComponentButton;
