import { unFavorites } from '@/api/favorites';
import useLocale from '@/utils/useLocale';
import { Button, Message, Tooltip } from '@arco-design/web-react';
import { IconDelete } from '@arco-design/web-react/icon';
import React, { useState } from 'react';
import i18n from './locale';

const UnFavorites = ({ item, onSucces }) => {
  const t = useLocale(i18n);
  const [loading, setLoading] = useState(false);
  const handleClick = () => {
    setLoading(true);
    unFavorites([item.id])
      .then(() => {
        Message.success(t['success.message']);
        onSucces();
      })
      .finally(() => setLoading(false));
  };
  return (
    <Tooltip content={t['title']}>
      <Button
        size="small"
        type="text"
        loading={loading}
        onClick={handleClick}
        status="danger"
        icon={<IconDelete />}
      />
    </Tooltip>
  );
};

export default UnFavorites;
