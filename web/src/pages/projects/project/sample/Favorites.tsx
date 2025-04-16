import React, { useState } from 'react';
import useUrlParams from '../hooks/useUrlParams';
import { doFavorite } from '@/api/favorites';
import { Button, Message } from '@arco-design/web-react';
import { IconStarFill } from '@arco-design/web-react/icon';
import useLocale from '@/utils/useLocale';
import i18n from './locale';

interface FavoritesProps {
  rootComponentId: string;
  componentIds: string[];
  disabled?: boolean;
}
const Favorites = ({
  rootComponentId,
  componentIds,
  disabled,
}: FavoritesProps) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const [loading, setLoading] = useState(false);
  const onFavorites = () => {
    setLoading(true);
    doFavorite({ projectId, type: 'faq-root', rootComponentId, componentIds })
      .then(() => Message.success(t['sample.favorites.success']))
      .finally(() => setLoading(false));
  };
  return (
    <Button
      loading={loading}
      size="small"
      disabled={disabled}
      type="text"
      icon={<IconStarFill className="favorites-icon" />}
      onClick={onFavorites}
    />
  );
};

export default Favorites;
