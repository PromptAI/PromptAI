import { deleteComponent } from '@/api/components';
import useUrlParams from '@/pages/projects/project/hooks/useUrlParams';
import useLocale from '@/utils/useLocale';
import { Tooltip, Popconfirm, Button, Message } from '@arco-design/web-react';
import { IconDelete } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import i18n from './locale';

const DeleteColumn = ({ row, onSuccess }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [loading, setLoading] = useState(false);
  const deleteQA = (row) => {
    setLoading(true);
    deleteComponent(projectId, [row?.user?.id])
      .then(() => {
        Message.success(t['sample.delete.success']);
        onSuccess();
      })
      .finally(() => setLoading(false));
  };
  return (
    <Tooltip content={t['sample.delete']}>
      <Popconfirm
        title={`${t['sample.delete.placeholder']}`}
        onOk={() => deleteQA(row)}
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
