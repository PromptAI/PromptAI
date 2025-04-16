import useLocale from '@/utils/useLocale';
import { Tooltip, Popconfirm, Button, Message } from '@arco-design/web-react';
import { IconDelete } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import i18n from './locale';
import { deleteCycleTask } from '@/api/cycle-tasks';

const DeleteColumn = ({ row, onSuccess }) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useState(false);
  const deleteCycle = (row) => {
    setLoading(true);
    deleteCycleTask(row.id)
      .then(() => {
        Message.success(t['cycle.delete.success']);
        onSuccess();
      })
      .finally(() => setLoading(false));
  };
  return (
    <Tooltip content={t['cycle.delete']}>
      <Popconfirm
        title={`${t['cycle.delete.placeholder']}`}
        onOk={() => deleteCycle(row)}
        position="lt"
      >
        <Button
          loading={loading}
          size="small"
          type="text"
          status="danger"
          icon={<IconDelete />}
        />
      </Popconfirm>
    </Tooltip>
  );
};

export default DeleteColumn;
