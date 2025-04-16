import { deleteComponent } from '@/api/components';
import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { IconDelete } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import useUrlParams from '../../../hooks/useUrlParams';
import i18n from './locale';

const BacthDelete = ({ keys, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const { projectId } = useUrlParams();
  const onDeleteRows = () => {
    deleteComponent(projectId, keys)
      .then(() => {
        setLoading(true);
        Message.success(t['sample.delete.success']);
        onSuccess();
      })
      .finally(() => setLoading(false));
  };
  const t = useLocale(i18n);
  return (
    <Button
      type="text"
      icon={<IconDelete />}
      status="danger"
      onClick={onDeleteRows}
      loading={loading}
    >
      {t['sample.delete.batch']}
    </Button>
  );
};
export default BacthDelete;
