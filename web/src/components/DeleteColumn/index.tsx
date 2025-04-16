import {
  Button,
  ButtonProps,
  Message,
  Popconfirm,
} from '@arco-design/web-react';
import { IconDelete } from '@arco-design/web-react/icon';
import { useRequest } from 'ahooks';
import React from 'react';
import { DeleteColumnProps } from './type';

const DeleteColumn = ({
  row,
  dataIndex = 'id',
  promise,
  children,
  onSuccess,
  title,
  ...rest
}: DeleteColumnProps & Omit<ButtonProps, 'loading' | 'onClick'>) => {
  const { loading, run } = useRequest(() => promise([row[dataIndex]]), {
    manual: true,
    onSuccess,
    onError: (e) => {
      Message.error(e.message);
    },
  });
  return (
    <Popconfirm focusLock title={title} onOk={run}>
      <Button
        size="small"
        type="text"
        icon={<IconDelete />}
        status="danger"
        {...rest}
        loading={loading}
      >
        {children}
      </Button>
    </Popconfirm>
  );
};

export default DeleteColumn;
