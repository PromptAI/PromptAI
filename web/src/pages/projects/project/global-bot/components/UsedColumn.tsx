import useLocale from '@/utils/useLocale';
import { Space } from '@arco-design/web-react';
import { groupBy, isEmpty } from 'lodash';
import React, { useMemo } from 'react';
import { Link } from 'react-router-dom';
import useUrlParams from '../../hooks/useUrlParams';
import i18n from '../locale';
import RefTag from '@/components/RefTag';

const UsedColumn = ({ item }) => {
  const t = useLocale(i18n);
  const { projectId } = useUrlParams();
  const group = useMemo(
    () =>
      Object.values(
        groupBy(
          item?.componentRelation?.usedByComponentRoots || [],
          'rootComponentId'
        )
      )
        .map((i) => ({
          rootComponentId: i?.[0]?.rootComponentId,
          rootComponentName: i?.[0]?.rootComponentName,
          refLength: i?.length,
        }))
        .filter((i) => !isEmpty(i.rootComponentId)),
    [item?.componentRelation?.usedByComponentRoots]
  );

  return (
    <Space>
      {group.map(({ rootComponentId, rootComponentName, refLength }) => (
        <Link
          key={rootComponentId}
          to={`/projects/${projectId}/overview/complexs/${rootComponentId}/branch/complex`}
          className="no-underline"
        >
          <RefTag>
            {`(${rootComponentName}) ${t['globalBots.table.use']} ${refLength}`}
          </RefTag>
        </Link>
      ))}
    </Space>
  );
};

export default UsedColumn;
