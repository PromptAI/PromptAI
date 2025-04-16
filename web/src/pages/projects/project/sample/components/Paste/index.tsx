import { doFavoritePaste } from '@/api/favorites';
import useLocale from '@/utils/useLocale';
import { Button, Message } from '@arco-design/web-react';
import { cloneDeep, keyBy } from 'lodash';
import React, { useCallback, useState } from 'react';
import i18n from '../../locale';

const Paste = ({ frame, onSuccess, projectId, rootComponentId }) => {
  const [loading, setLoading] = useState(false);
  const t = useLocale(i18n);
  const handlePaste = useCallback(() => {
    let components = cloneDeep(frame.data.items);
    const idMap = keyBy(components, 'id');
    const root = components.find((c) => !idMap[c.parentId]);
    components = components.filter((c) => idMap[c.parentId]);
    if (root) {
      root.parentId = rootComponentId;
      components = [root, ...components].map((c) => ({
        ...c,
        projectId,
        rootComponentId,
      }));
      setLoading(true);
      doFavoritePaste(components, projectId, rootComponentId)
        .then(() => {
          Message.success(t['sample.favorites.apply.success']);
          onSuccess();
        })
        .finally(() => setLoading(false));
    }
  }, [frame.data.items, onSuccess, projectId, rootComponentId, t]);
  return (
    <Button type="primary" size="mini" loading={loading} onClick={handlePaste}>
      {t['sample.favorites.apply']}
    </Button>
  );
};

export default Paste;
