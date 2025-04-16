import React, { useMemo } from 'react';
import FaqFavoriteDetial from '@/pages/projects/project/favorites/faqs/components/Detail';
import { keyBy } from 'lodash';
import { Button } from '@arco-design/web-react';
import useLocale from '@/utils/useLocale';
import i18n from '../../locale';

const Detail = ({ frame }) => {
  const item = useMemo(() => {
    const { items: nodes } = frame.data;
    const nodeMap = keyBy(nodes, 'type');
    return {
      user: nodeMap['user'],
      bot: nodeMap['bot'],
    };
  }, [frame.data]);
  const t = useLocale(i18n);
  return (
    <FaqFavoriteDetial
      row={item}
      trigger={
        <Button type="text" size="mini">
          {t['sample.favorites.detail']}
        </Button>
      }
    />
  );
};

export default Detail;
